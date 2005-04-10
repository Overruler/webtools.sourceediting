/*****************************************************************************
 * Copyright (c) 2004 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and
 * is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************/
package org.eclipse.wst.css.ui.contentassist;



import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.wst.css.core.internal.provisional.adapters.ICSSModelAdapter;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSDocument;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSModel;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.html.core.htmlcss.StyleAdapterFactory;
import org.eclipse.wst.sse.core.INodeAdapter;
import org.eclipse.wst.sse.core.IStructuredModel;
import org.eclipse.wst.sse.core.IndexedRegion;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;
import org.eclipse.wst.xml.core.document.IDOMNode;
import org.eclipse.wst.xml.ui.contentassist.XMLContentAssistUtilities;
import org.eclipse.wst.xml.ui.util.SharedXMLEditorPluginImageHelper;

public class CSSContentAssistProcessor implements IContentAssistProcessor {

	private int fDocumentOffset = 0;
	private char fQuote = 0;

	/**
	 * Return a list of proposed code completions based on the specified
	 * location within the document that corresponds to the current cursor
	 * position within the text-editor control.
	 * 
	 * @param documentPosition
	 *            a location within the document
	 * @return an array of code-assist items
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentPosition) {

		IndexedRegion indexedNode = ContentAssistUtils.getNodeAt((StructuredTextViewer) viewer, documentPosition + fDocumentOffset);
		IDOMNode xNode = null;
		IDOMNode parent = null;
		CSSProposalArranger arranger = null;

		// bail if we couldn't get an indexed node
		// if(indexedNode == null) return new ICompletionProposal[0];
		if (indexedNode instanceof IDOMNode) {
			xNode = (IDOMNode) indexedNode;
			parent = (IDOMNode) xNode.getParentNode();
		}
		// need to get in here if there in the no 0 region <style>|</style>
		// case
		if (xNode != null && xNode.getNodeName().equalsIgnoreCase(HTML40Namespace.ElementName.STYLE)) {
			// now we know the cursor is in a <style> tag w/out region
			IStructuredModel cssModel = getCSSModel(xNode);
			if (cssModel != null) {
				// adjust offsets for embedded style
				int offset = documentPosition;
				int pos = 0;
				IndexedRegion keyIndexedNode = cssModel.getIndexedRegion(pos);
				if (keyIndexedNode == null) {
					keyIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
				}
				arranger = new CSSProposalArranger(pos, (ICSSNode) keyIndexedNode, offset, (char) 0);
			}
		}
		else if (parent != null && parent.getNodeName().equalsIgnoreCase(HTML40Namespace.ElementName.STYLE)) {
			// now we know the cursor is in a <style> tag with a region
			// use the parent because that will be the <style> tag
			IStructuredModel cssModel = getCSSModel(parent);
			if (cssModel != null) {
				// adjust offsets for embedded style
				int offset = indexedNode.getStartOffset();
				int pos = documentPosition - offset;
				IndexedRegion keyIndexedNode = cssModel.getIndexedRegion(pos);
				if (keyIndexedNode == null) {
					keyIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
				}
				arranger = new CSSProposalArranger(pos, (ICSSNode) keyIndexedNode, offset, (char) 0);
			}
		}
		else if (indexedNode instanceof IDOMNode) {
			// get model for node w/ style attribute
			IStructuredModel cssModel = getCSSModel((IDOMNode) indexedNode);
			if (cssModel != null) {
				IndexedRegion keyIndexedNode = cssModel.getIndexedRegion(documentPosition - fDocumentOffset);
				if (keyIndexedNode == null) {
					keyIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
				}
				if (keyIndexedNode instanceof ICSSNode) {
					// inline style for a tag, not embedded
					arranger = new CSSProposalArranger(documentPosition, (ICSSNode) keyIndexedNode, fDocumentOffset, fQuote);
				}
			}
		}
		else if (indexedNode instanceof ICSSNode) {
			// when editing external CSS using CSS Designer, ICSSNode is
			// passed.
			ICSSDocument cssdoc = ((ICSSNode) indexedNode).getOwnerDocument();
			if (cssdoc != null) {
				IStructuredModel cssModel = cssdoc.getModel();
				if (cssModel != null) {
					IndexedRegion keyIndexedNode = cssModel.getIndexedRegion(documentPosition - fDocumentOffset);
					if (keyIndexedNode == null) {
						keyIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
					}
					if (keyIndexedNode instanceof ICSSNode) {
						// inline style for a tag, not embedded
						arranger = new CSSProposalArranger(documentPosition, (ICSSNode) keyIndexedNode, fDocumentOffset, fQuote);
					}
				}
			}
		}
		else if (indexedNode == null && isViewerEmpty(viewer)) {
			// the top of empty CSS Document
			IStructuredModel cssModel = null;
			try {
				cssModel = StructuredModelManager.getModelManager().getExistingModelForRead(viewer.getDocument());
				if (cssModel instanceof ICSSModel) {
					IndexedRegion keyIndexedNode = cssModel.getIndexedRegion(documentPosition - fDocumentOffset);
					if (keyIndexedNode == null) {
						keyIndexedNode = (IndexedRegion) ((ICSSModel) cssModel).getDocument();
					}
					if (keyIndexedNode instanceof ICSSNode) {
						// inline style for a tag, not embedded
						arranger = new CSSProposalArranger(documentPosition, (ICSSNode) keyIndexedNode, fDocumentOffset, fQuote);
					}
				}
			}
			finally {
				if (cssModel != null)
					cssModel.releaseFromRead();
			}
		}

		ICompletionProposal[] proposals = new ICompletionProposal[0];
		if (arranger != null) {
			fDocumentOffset = 0;
			proposals = arranger.getProposals();

			// add end tag if parent is not closed
			ICompletionProposal endTag = XMLContentAssistUtilities.computeXMLEndTagProposal(viewer, documentPosition, indexedNode, HTML40Namespace.ElementName.STYLE, SharedXMLEditorPluginImageHelper.IMG_OBJ_TAG_GENERIC); //$NON-NLS-1$
			if (endTag != null) {
				ICompletionProposal[] plusOne = new ICompletionProposal[proposals.length + 1];
				System.arraycopy(proposals, 0, plusOne, 1, proposals.length);
				plusOne[0] = endTag;
				proposals = plusOne;
			}
		}
		return proposals;
	}

	/**
	 * Returns true if there is no text or it's all white space, otherwise
	 * returns false
	 * 
	 * @param treeNode
	 * @param textViewer
	 * @return boolean
	 */
	private boolean isViewerEmpty(ITextViewer textViewer) {
		boolean isEmpty = false;
		String text = textViewer.getTextWidget().getText();
		if (text == null || (text != null && text.trim().equals(""))) //$NON-NLS-1$
			isEmpty = true;
		return isEmpty;
	}

	/**
	 * Get CSSModel for an indexed node
	 * 
	 * @param indexedNode
	 * @return IStructuredModel
	 */
	// private IStructuredModel getCSSModel(IndexedRegion indexedNode) {
	// if (indexedNode == null) return null;
	// Node node = (Node)indexedNode;
	// INodeNotifier notifier = (INodeNotifier)node.getParentNode();
	// if (notifier == null) return null;
	// INodeAdapter adapter =
	// StyleAdapterFactory.getInstance().adapt(notifier);
	// if (adapter == null || !(adapter instanceof CSSModelAdapter)) return
	// null;
	// CSSModelAdapter modelAdapter = (CSSModelAdapter)adapter;
	// return modelAdapter.getModel();
	// }
	/**
	 * Returns the CSSmodel for a given XML node.
	 * 
	 * @param element
	 * @return IStructuredModel
	 */
	private IStructuredModel getCSSModel(IDOMNode element) {
		if (element == null)
			return null;
		INodeAdapter adapter = StyleAdapterFactory.getInstance().adapt(element);
		if (adapter == null || !(adapter instanceof ICSSModelAdapter))
			return null;
		ICSSModelAdapter modelAdapter = (ICSSModelAdapter) adapter;
		return modelAdapter.getModel();
	}

	/**
	 * Returns information about possible contexts based on the specified
	 * location within the document that corresponds to the current cursor
	 * position within the text viewer.
	 * 
	 * @param viewer
	 *            the viewer whose document is used to compute the possible
	 *            contexts
	 * @param documentPosition
	 *            an offset within the document for which context information
	 *            should be computed
	 * @return an array of context information objects or <code>null</code>
	 *         if no context could be found
	 */
	public org.eclipse.jface.text.contentassist.IContextInformation[] computeContextInformation(org.eclipse.jface.text.ITextViewer viewer, int documentOffset) {
		return null;
	}

	/**
	 * Returns the characters which when entered by the user should
	 * automatically trigger the presentation of possible completions.
	 * 
	 * @return the auto activation characters for completion proposal or
	 *         <code>null</code> if no auto activation is desired
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	/**
	 * Returns the characters which when entered by the user should
	 * automatically trigger the presentation of context information.
	 * 
	 * @return the auto activation characters for presenting context
	 *         information or <code>null</code> if no auto activation is
	 *         desired
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/**
	 * Returns a validator used to determine when displayed context
	 * information should be dismissed. May only return <code>null</code> if
	 * the processor is incapable of computing context information.
	 * 
	 * @return a context information validator, or <code>null</code> if the
	 *         processor is incapable of computing context information
	 */
	public org.eclipse.jface.text.contentassist.IContextInformationValidator getContextInformationValidator() {
		return null;
	}

	/**
	 * Return the reason why computeProposals was not able to find any
	 * completions.
	 * 
	 * @return an error message or null if no error occurred
	 */
	public String getErrorMessage() {
		return null;
	}

	/**
	 * Insert the method's description here. Creation date: (2001/05/22
	 * 10:37:05)
	 * 
	 * @param offset
	 *            int
	 */
	public void setDocumentOffset(int offset) {
		fDocumentOffset = offset;
	}

	/**
	 * 
	 * @param quote
	 *            char
	 */
	public void setQuoteCharOfStyleAttribute(char quote) {
		fQuote = quote;
	}
}