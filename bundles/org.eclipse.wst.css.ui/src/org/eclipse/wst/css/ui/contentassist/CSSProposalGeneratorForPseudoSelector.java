/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.contentassist;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.wst.css.core.internal.metamodel.CSSMMNode;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMSelector;
import org.eclipse.wst.css.core.internal.metamodel.util.CSSMMTypeCollector;
import org.eclipse.wst.css.core.internal.parser.CSSRegionUtil;
import org.eclipse.wst.css.core.internal.parserz.CSSRegionContexts;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSPageRule;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSStyleRule;
import org.eclipse.wst.css.core.preferences.CSSPreferenceHelper;
import org.eclipse.wst.css.ui.image.CSSImageType;
import org.eclipse.wst.sse.core.text.ITextRegion;

/**
 *  
 */
class CSSProposalGeneratorForPseudoSelector extends CSSProposalGenerator {

	/**
	 * CSSProposalGeneratorForPseudoSelector constructor comment.
	 * 
	 * @param context
	 *            com.ibm.sed.contentassist.old.css.CSSContentAssistContext
	 */
	CSSProposalGeneratorForPseudoSelector(CSSContentAssistContext context) {
		super(context);
	}

	/**
	 * getCandidates method comment.
	 */
	protected Iterator getCandidates() {
		List candidates = new ArrayList();

		boolean hasLeadingColon = checkLeadingColon();
		String textToReplace = fContext.getTextToReplace();
		if (!hasLeadingColon && 0 < textToReplace.length() && !textToReplace.equals(fContext.getTextToCompare())) {
			// cursor placed midpoint of the region
			return candidates.iterator();
		}
		ITextRegion region = fContext.getTargetRegion();
		if (region != null) {
			String type = region.getType();
			if (type != CSSRegionContexts.CSS_S && !CSSRegionUtil.isSelectorBegginingType(type)) {
				return candidates.iterator();
			}
		}

		boolean useUpperCase = CSSPreferenceHelper.getInstance().isIdentUpperCase();

		List tags = getSelectorTags();
		Collections.sort(tags);
		Iterator i = tags.iterator();
		while (i.hasNext()) {
			String text = (String) i.next();
			if (hasLeadingColon && !isMatch(text)) {
				continue;
			}
			text = (useUpperCase) ? text.toUpperCase() : text.toLowerCase();

			int cursorPos = 0;
			StringBuffer buf = new StringBuffer();
			if (hasLeadingColon) {
				buf.append(text.substring(1));
			} else {
				buf.append(textToReplace);
				buf.append(text);
			}
			cursorPos += buf.length();

			if (0 < buf.length()) {
				boolean inRule = (fContext.getTargetNode() instanceof ICSSStyleRule || fContext.getTargetNode() instanceof ICSSPageRule);
				if (!inRule || (textToReplace.length() == 0 && !hasLeadingColon)) {
					buf.append(" ");//$NON-NLS-1$
					cursorPos += 1;
				}
				if (!inRule) {
					StringAndOffset sao = generateBraces();
					buf.append(sao.fString);
					cursorPos += sao.fOffset;
				}
				CSSCACandidate item = new CSSCACandidate();
				item.setReplacementString(buf.toString());
				item.setCursorPosition(cursorPos);
				item.setDisplayString(text);
				item.setImageType(CSSImageType.SELECTOR_PSEUDO);
				candidates.add(item);
			}
		}

		return candidates.iterator();
	}

	/**
	 *  
	 */
	List getSelectorTags() {
		List tagList = new ArrayList();
		ICSSNode targetNode = fContext.getTargetNode();
		String rootType = (targetNode instanceof ICSSPageRule) ? CSSMMNode.TYPE_PAGE_RULE : CSSMMNode.TYPE_STYLE_RULE;

		CSSMMTypeCollector collector = new CSSMMTypeCollector();
		collector.collectNestedType(false);
		collector.apply(fContext.getMetaModel(), rootType);
		Iterator i;
		i = collector.getNodes();
		if (!i.hasNext()) {
			return tagList;
		}
		CSSMMNode node = (CSSMMNode) i.next();
		i = node.getChildNodes();
		while (i.hasNext()) {
			CSSMMNode child = (CSSMMNode) i.next();
			if (child.getType() == CSSMMNode.TYPE_SELECTOR) {
				String selType = ((CSSMMSelector) child).getSelectorType();
				if (selType == CSSMMSelector.TYPE_PSEUDO_CLASS || selType == CSSMMSelector.TYPE_PSEUDO_ELEMENT) {
					tagList.add(((CSSMMSelector) child).getSelectorString());
				}
			}
		}
		return tagList;
	}

	/**
	 *  
	 */
	protected boolean isMatch(String text) {
		if (!super.isMatch(text)) {
			ITextRegion region = fContext.getTargetRegion();
			if (region != null && region.getType() == CSSRegionContexts.CSS_SELECTOR_PSEUDO) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}
}