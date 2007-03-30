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
package org.eclipse.wst.jsdt.web.ui.internal.breakpointproviders;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.wst.jsdt.debug.core.JDIDebugModel;
import org.eclipse.wst.jsdt.web.core.text.IJSPPartitions;
import org.eclipse.wst.jsdt.web.ui.internal.JSPUIMessages;
import org.eclipse.wst.jsdt.web.ui.internal.JSPUIPlugin;
import org.eclipse.wst.sse.ui.internal.StructuredResourceMarkerAnnotationModel;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.ISourceEditingTextTools;
import org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint.IBreakpointProvider;

/**
 * A IBreakpointProvider supporting JSP breakpoints for a Non-Java Language
 * Source JSP page
 */
public class JavaStratumBreakpointProvider implements IBreakpointProvider,
		IExecutableExtension {
	private String fClassPattern = null;

	public IStatus addBreakpoint(IDocument document, IEditorInput input,
			int editorLineNumber, int offset) throws CoreException {
		// check if there is a valid position to set breakpoint
		int pos = getValidPosition(document, editorLineNumber);
		IStatus status = null;
		if (pos >= 0) {
			IResource res = getResourceFromInput(input);
			if (res != null) {
				String path = null; // res.getName();//
									// res.getFullPath().removeFirstSegments(2).toString();
				IBreakpoint point = JDIDebugModel
						.createStratumBreakpoint(
								res,
								"JSP", res.getName(), path, getClassPattern(res), editorLineNumber, pos, pos, 0, true, null); //$NON-NLS-1$
				if (point == null) {
					status = new Status(IStatus.ERROR, JSPUIPlugin.ID,
							IStatus.ERROR, "unsupported input type", null); //$NON-NLS-1$
				}
			} else if (input instanceof IStorageEditorInput) {
				// For non-resources, use the workspace root and a coordinated
				// attribute that is used to
				// prevent unwanted (breakpoint) markers from being loaded
				// into the editors.
				res = ResourcesPlugin.getWorkspace().getRoot();
				String id = input.getName();
				if (input instanceof IStorageEditorInput
						&& ((IStorageEditorInput) input).getStorage() != null
						&& ((IStorageEditorInput) input).getStorage()
								.getFullPath() != null) {
					id = ((IStorageEditorInput) input).getStorage()
							.getFullPath().toString();
				}
				Map attributes = new HashMap();
				attributes
						.put(
								StructuredResourceMarkerAnnotationModel.SECONDARY_ID_KEY,
								id);
				String path = null;
				IBreakpoint point = JDIDebugModel
						.createStratumBreakpoint(
								res,
								"JSP", input.getName(), path, getClassPattern(res), editorLineNumber, pos, pos, 0, true, attributes); //$NON-NLS-1$
				if (point == null) {
					status = new Status(IStatus.ERROR, JSPUIPlugin.ID,
							IStatus.ERROR, "unsupported input type", null); //$NON-NLS-1$
				}
			}
		}
		if (status == null) {
			status = new Status(IStatus.OK, JSPUIPlugin.ID, IStatus.OK,
					JSPUIMessages.OK, null);
		}
		return status;
	}

	private String getClassPattern(IResource resource) {
		if (resource != null) {
			String shortName = resource.getName();
			String extension = resource.getFileExtension();
			if (extension != null
					&& extension.length() > shortName.length() - 1) {
				shortName = shortName.substring(0, shortName.length()
						- extension.length() - 1);
			}
			/*
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=154475
			 */
			return fClassPattern + ",_" + shortName;
		}
		return fClassPattern;
	}

	public IResource getResource(IEditorInput input) {
		return getResourceFromInput(input);
	}

	private IResource getResourceFromInput(IEditorInput input) {
		IResource resource = (IResource) input.getAdapter(IFile.class);
		if (resource == null) {
			resource = (IResource) input.getAdapter(IResource.class);
		}
		return resource;
	}

	/**
	 * Finds a valid position somewhere on lineNumber in document, idoc, where a
	 * breakpoint can be set and returns that position. -1 is returned if a
	 * position could not be found.
	 * 
	 * @param idoc
	 * @param editorLineNumber
	 * @return position to set breakpoint or -1 if no position could be found
	 */
	private int getValidPosition(IDocument idoc, int editorLineNumber) {
		int result = -1;
		if (idoc != null) {

			int startOffset = 0;
			int endOffset = 0;
			try {
				IRegion line = idoc.getLineInformation(editorLineNumber - 1);
				startOffset = line.getOffset();
				endOffset = Math.max(line.getOffset(), line.getOffset()
						+ line.getLength());

				String lineText = idoc
						.get(startOffset, endOffset - startOffset).trim();

				// blank lines or lines with only an open or close brace or
				// scriptlet tag cannot have a breakpoint
				if (lineText.equals("") || lineText.equals("{") || //$NON-NLS-1$ //$NON-NLS-2$
						lineText.equals("}") || lineText.equals("<%")) //$NON-NLS-1$ //$NON-NLS-2$
				{
					result = -1;
				} else {
					// get all partitions for current line
					ITypedRegion[] partitions = null;

					partitions = idoc.computePartitioning(startOffset,
							endOffset - startOffset);

					for (int i = 0; i < partitions.length; ++i) {
						String type = partitions[i].getType();
						// if found jsp java content, jsp directive tags,
						// custom
						// tags,
						// return that position
						if (type == IJSPPartitions.JSP_CONTENT_JAVA
								|| type == IJSPPartitions.JSP_DIRECTIVE) {
							result = partitions[i].getOffset();
						}
					}
				}
			} catch (BadLocationException e) {
				result = -1;
			}
		}

		return result;
	}

	/**
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement,
	 *      java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		if (data != null) {
			if (data instanceof String && data.toString().length() > 0) {
				fClassPattern = (String) data;
			}
		}
	}

	public void setSourceEditingTextTools(ISourceEditingTextTools tools) {
		// not used
	}
}