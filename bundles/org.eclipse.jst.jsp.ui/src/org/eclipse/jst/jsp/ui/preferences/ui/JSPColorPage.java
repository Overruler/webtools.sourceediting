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
package org.eclipse.jst.jsp.ui.preferences.ui;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.jst.jsp.ui.internal.editor.IHelpContextIds;
import org.eclipse.jst.jsp.ui.internal.nls.ResourceHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.wst.common.encoding.content.IContentTypeIdentifier;
import org.eclipse.wst.html.ui.style.IStyleConstantsHTML;
import org.eclipse.wst.sse.core.IModelManager;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.wst.sse.ui.preferences.PreferenceKeyGenerator;
import org.eclipse.wst.sse.ui.preferences.ui.StyledTextColorPicker;
import org.eclipse.wst.xml.core.jsp.model.parser.temp.XMLJSPRegionContexts;
import org.eclipse.wst.xml.core.parser.XMLRegionContext;
import org.eclipse.wst.xml.ui.preferences.XMLColorPage;
import org.eclipse.wst.xml.ui.style.IStyleConstantsXML;

public class JSPColorPage extends XMLColorPage {

	/**
	 * Set up all the style preference keys in the overlay store
	 */
	protected OverlayKey[] createOverlayStoreKeys() {
		ArrayList overlayKeys = new ArrayList();
		
		ArrayList styleList = new ArrayList();
		initStyleList(styleList);
		Iterator i = styleList.iterator();
		while (i.hasNext()) {
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, PreferenceKeyGenerator.generateKey((String)i.next(), IContentTypeIdentifier.ContentTypeID_JSP)));	
		}

		OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return keys;
	}

	public String getSampleText() {
		return ResourceHandler.getString("Sample_JSP_doc"); //$NON-NLS-1$ = "<%@ page \n\tlanguage=\"java\" \n\tcontentType=\"text/html; charset=ISO-8859-1\"\n%>\n<jsp:include flush=\"true\" page=\"titleBar.jsp\"/>\n\n<%-- Use below tags ONLY for JSP 1.1 --%>\n<%\n\tSystem.out.println(\"Welcome!\");\n%>\n<%-- Use below tags ONLY for JSP 1.2 --%>\n<jsp:scriptlet>\n\tSystem.out.println(\"Welcome!\");\n</jsp:scriptlet>"
	}

	protected void initContextStyleMap(Dictionary contextStyleMap) {

		initCommonContextStyleMap(contextStyleMap);
		contextStyleMap.remove(XMLRegionContext.XML_CONTENT); // leave content between tags alone
		contextStyleMap.remove(XMLRegionContext.XML_DECLARATION_OPEN); // xml/html specific
		contextStyleMap.remove(XMLRegionContext.XML_DECLARATION_CLOSE); // xml/html specific
		contextStyleMap.remove(XMLRegionContext.XML_ELEMENT_DECLARATION); // xml/html specific
		contextStyleMap.remove(XMLRegionContext.XML_ELEMENT_DECL_CLOSE); // xml/html specific

		//	contextStyleMap.put(XMLJSPRegionContexts.JSP_CONTENT, HTMLColorManager.SCRIPT_AREA);
		//	contextStyleMap.put(XMLJSPRegionContexts.BLOCK_TEXT, HTMLColorManager.SCRIPT_AREA);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_DECLARATION_OPEN, IStyleConstantsHTML.SCRIPT_AREA_BORDER);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_SCRIPTLET_OPEN, IStyleConstantsHTML.SCRIPT_AREA_BORDER);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_EXPRESSION_OPEN, IStyleConstantsHTML.SCRIPT_AREA_BORDER);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_DIRECTIVE_OPEN, IStyleConstantsHTML.SCRIPT_AREA_BORDER);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_DIRECTIVE_CLOSE, IStyleConstantsHTML.SCRIPT_AREA_BORDER);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_CLOSE, IStyleConstantsHTML.SCRIPT_AREA_BORDER);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_DIRECTIVE_NAME, IStyleConstantsXML.TAG_NAME);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_ROOT_TAG_NAME, IStyleConstantsXML.TAG_NAME);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_COMMENT_OPEN, IStyleConstantsXML.COMMENT_BORDER);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_COMMENT_TEXT, IStyleConstantsXML.COMMENT_TEXT);
		contextStyleMap.put(XMLJSPRegionContexts.JSP_COMMENT_CLOSE, IStyleConstantsXML.COMMENT_BORDER);

		contextStyleMap.put(XMLJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_DQUOTE, IStyleConstantsXML.TAG_ATTRIBUTE_VALUE);
		contextStyleMap.put(XMLJSPRegionContexts.XML_TAG_ATTRIBUTE_VALUE_SQUOTE, IStyleConstantsXML.TAG_ATTRIBUTE_VALUE);
	}

	protected void initDescriptions(Dictionary descriptions) {
		initCommonDescriptions(descriptions);
		descriptions.remove(IStyleConstantsXML.XML_CONTENT); // leave content between tags alone
		descriptions.remove(IStyleConstantsXML.DECL_BORDER); // xml/html specific
		descriptions.put(IStyleConstantsHTML.SCRIPT_AREA_BORDER, ResourceHandler.getString("JSP_Delimiters_UI_")); //$NON-NLS-1$ = "JSP Delimiters"
	}

	protected void initStyleList(ArrayList list) {
		initCommonStyleList(list);
		list.remove(IStyleConstantsXML.XML_CONTENT); // leave content between tags alone
		list.remove(IStyleConstantsXML.DECL_BORDER); // xml/html specific
		list.add(IStyleConstantsHTML.SCRIPT_AREA_BORDER);
	}

	protected void setupPicker(StyledTextColorPicker picker) {
		IModelManager mmanager = StructuredModelManager.getInstance().getModelManager();
		picker.setParser(mmanager.createStructuredDocumentFor(IContentTypeIdentifier.ContentTypeID_JSP).getParser());

		// create descriptions for hilighting types
		Dictionary descriptions = new Hashtable();
		initDescriptions(descriptions);

		// map region types to hilighting types
		Dictionary contextStyleMap = new Hashtable();
		initContextStyleMap(contextStyleMap);

		ArrayList styleList = new ArrayList();
		initStyleList(styleList);

		picker.setContextStyleMap(contextStyleMap);
		picker.setDescriptions(descriptions);
		picker.setStyleList(styleList);

		picker.setGeneratorKey(IContentTypeIdentifier.ContentTypeID_JSP);
		//	updatePickerFont(picker);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Control c = super.createContents(parent);
		WorkbenchHelp.setHelp(c, IHelpContextIds.JSP_PREFWEBX_STYLES_HELPID);
		return c;
	}
}