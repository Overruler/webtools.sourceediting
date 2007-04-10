/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jens Lukowski/Innoopract - initial renaming/restructuring
 *     
 *******************************************************************************/
package org.eclipse.jst.jsp.core.internal.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jst.jsp.core.internal.JSPCoreMessages;
import org.eclipse.jst.jsp.core.internal.Logger;
import org.eclipse.jst.jsp.core.internal.provisional.JSP11Namespace;
import org.eclipse.jst.jsp.core.internal.provisional.JSP20Namespace;
import org.eclipse.jst.jsp.core.internal.regions.DOMJSPRegionContexts;
import org.eclipse.jst.jsp.core.taglib.ITaglibRecord;
import org.eclipse.jst.jsp.core.taglib.TaglibIndex;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.validation.internal.core.ValidationException;
import org.eclipse.wst.validation.internal.provisional.core.IMessage;
import org.eclipse.wst.validation.internal.provisional.core.IReporter;
import org.eclipse.wst.validation.internal.provisional.core.IValidationContext;
import org.eclipse.wst.validation.internal.provisional.core.IValidator;

import com.ibm.icu.text.Collator;

/**
 * Checks for: - duplicate taglib prefix values and reserved taglib prefix
 * values in the same file
 */
public class JSPDirectiveValidator extends JSPValidator {
	private static Collator collator = Collator.getInstance(Locale.US);

	private static final boolean DEBUG = Boolean.valueOf(Platform.getDebugOption("org.eclipse.jst.jsp.core/debug/jspvalidator")).booleanValue(); //$NON-NLS-1$
	private IValidator fMessageOriginator;
	private HashMap fPrefixValueRegionToDocumentRegionMap = new HashMap();

	private HashMap fReservedPrefixes = new HashMap();
	private int fSeverityIncludeFileMissing = IMessage.NORMAL_SEVERITY;
	private int fSeverityIncludeFileNotSpecified = IMessage.NORMAL_SEVERITY;
	private int fSeverityTaglibDuplicatePrefixWithDifferentURIs = IMessage.NORMAL_SEVERITY;
	private int fSeverityTaglibDuplicatePrefixWithSameURIs = IMessage.LOW_SEVERITY;
	private int fSeverityTaglibMissingPrefix = IMessage.HIGH_SEVERITY;
	private int fSeverityTaglibMissingURI = IMessage.HIGH_SEVERITY;
	private int fSeverityTaglibUnresolvableURI = IMessage.HIGH_SEVERITY;

	private HashMap fTaglibPrefixesInUse = new HashMap();
	private final int NO_SEVERITY = -1;

	public JSPDirectiveValidator() {
		initReservedPrefixes();
		fMessageOriginator = this;
	}

	public JSPDirectiveValidator(IValidator validator) {
		initReservedPrefixes();
		this.fMessageOriginator = validator;
	}

	public void cleanup(IReporter reporter) {
		super.cleanup(reporter);
		fTaglibPrefixesInUse.clear();
		fPrefixValueRegionToDocumentRegionMap.clear();
	}

	private void collectTaglibPrefix(IStructuredDocumentRegion documentRegion, ITextRegion valueRegion, String taglibPrefix) {
		fPrefixValueRegionToDocumentRegionMap.put(valueRegion, documentRegion);

		Object o = fTaglibPrefixesInUse.get(taglibPrefix);
		if (o == null) {
			// prefix doesn't exist, remember it
			fTaglibPrefixesInUse.put(taglibPrefix, valueRegion);
		}
		else {
			List regionList = null;
			// already a List
			if (o instanceof List) {
				regionList = (List) o;
			}
			/*
			 * a single value region, create a new List and add previous
			 * valueRegion
			 */
			else {
				regionList = new ArrayList();
				regionList.add(o);
				fTaglibPrefixesInUse.put(taglibPrefix, regionList);
			}
			regionList.add(valueRegion);
		}
	}

	private void initReservedPrefixes() {
		fReservedPrefixes.put("jsp", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fReservedPrefixes.put("jspx", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fReservedPrefixes.put("java", ""); //$NON-NLS-1$ //$NON-NLS-2$
		fReservedPrefixes.put("javax", ""); //$NON-NLS-1$ //$NON-NLS-2$ 
		fReservedPrefixes.put("servlet", ""); //$NON-NLS-1$ //$NON-NLS-2$ 
		fReservedPrefixes.put("sun", ""); //$NON-NLS-1$ //$NON-NLS-2$ 
		fReservedPrefixes.put("sunw", ""); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	private boolean isReservedTaglibPrefix(String name) {
		return fReservedPrefixes.get(name) != null;
	}

	protected void performValidation(IFile f, IReporter reporter, IStructuredDocument sDoc) {
		/*
		 * when validating an entire file need to clear dupes or else you're
		 * comparing between files
		 */
		fPrefixValueRegionToDocumentRegionMap.clear();
		fTaglibPrefixesInUse.clear();

		// iterate all document regions
		IStructuredDocumentRegion region = sDoc.getFirstStructuredDocumentRegion();
		while (region != null && !reporter.isCancelled()) {
			// only checking directives
			if (region.getType() == DOMJSPRegionContexts.JSP_DIRECTIVE_NAME) {
				processDirective(reporter, f, sDoc, region);
			}
			region = region.getNext();
		}

		if (!reporter.isCancelled()) {
			reportTaglibDuplicatePrefixes(f, reporter, sDoc);
		}

		fPrefixValueRegionToDocumentRegionMap.clear();
		fTaglibPrefixesInUse.clear();
	}

	private void processDirective(IReporter reporter, IFile file, IStructuredDocument sDoc, IStructuredDocumentRegion documentRegion) {
		String directiveName = getDirectiveName(documentRegion);
		// we only care about taglib directive
		if (directiveName.equals("taglib")) { //$NON-NLS-1$
			processTaglibDirective(reporter, file, sDoc, documentRegion);
		}
		else if (directiveName.equals("include")) { //$NON-NLS-1$
			processIncludeDirective(reporter, file, sDoc, documentRegion);
		}
		// else if (directiveName.equals("page")) { //$NON-NLS-1$
		// }
	}

	private void processIncludeDirective(IReporter reporter, IFile file, IStructuredDocument sDoc, IStructuredDocumentRegion documentRegion) {
		ITextRegion fileValueRegion = getAttributeValueRegion(documentRegion, JSP11Namespace.ATTR_NAME_FILE);
		if (fileValueRegion != null) {
			// file specified
			String fileValue = documentRegion.getText(fileValueRegion);
			fileValue = StringUtils.stripQuotes(fileValue);

			if (fileValue.length() == 0 && fSeverityIncludeFileNotSpecified != NO_SEVERITY) {
				// file value is specified but empty
				String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_3, JSP11Namespace.ATTR_NAME_FILE);
				LocalizedMessage message = new LocalizedMessage(fSeverityIncludeFileNotSpecified, msgText, file);
				int start = documentRegion.getStartOffset(fileValueRegion);
				int length = fileValueRegion.getTextLength();
				int lineNo = sDoc.getLineOfOffset(start);
				message.setLineNo(lineNo);
				message.setOffset(start);
				message.setLength(length);

				reporter.addMessage(fMessageOriginator, message);
			}
			else if (fSeverityIncludeFileMissing != NO_SEVERITY) {
				IPath testPath = null;
				if (fileValue.startsWith("/")) {
					testPath = TaglibIndex.getContextRoot(file.getFullPath()).append(new Path(fileValue));
				}
				else {
					testPath = file.getFullPath().removeLastSegments(1).append(new Path(fileValue));
				}
				IFile testFile = file.getWorkspace().getRoot().getFile(testPath);
				if (!testFile.isAccessible()) {
					// File not found
					String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_4, new String[]{fileValue, testPath.toString()});
					LocalizedMessage message = new LocalizedMessage(fSeverityIncludeFileMissing, msgText, file);
					int start = documentRegion.getStartOffset(fileValueRegion);
					int length = fileValueRegion.getTextLength();
					int lineNo = sDoc.getLineOfOffset(start);
					message.setLineNo(lineNo);
					message.setOffset(start);
					message.setLength(length);

					reporter.addMessage(fMessageOriginator, message);
				}
			}
		}
		else if (fSeverityIncludeFileNotSpecified != NO_SEVERITY) {
			// file is not specified at all
			String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_3, JSP11Namespace.ATTR_NAME_FILE);
			LocalizedMessage message = new LocalizedMessage(fSeverityIncludeFileNotSpecified, msgText, file);
			int start = documentRegion.getStartOffset();
			int length = documentRegion.getTextLength();
			int lineNo = sDoc.getLineOfOffset(start);
			message.setLineNo(lineNo);
			message.setOffset(start);
			message.setLength(length);

			reporter.addMessage(fMessageOriginator, message);
		}
	}

	private void processTaglibDirective(IReporter reporter, IFile file, IStructuredDocument sDoc, IStructuredDocumentRegion documentRegion) {
		ITextRegion prefixValueRegion = null;
		ITextRegion uriValueRegion = getAttributeValueRegion(documentRegion, JSP11Namespace.ATTR_NAME_URI);
		ITextRegion tagdirValueRegion = getAttributeValueRegion(documentRegion, JSP20Namespace.ATTR_NAME_TAGDIR);
		if (uriValueRegion != null) {
			// URI is specified
			String uri = documentRegion.getText(uriValueRegion);

			if (file != null) {
				uri = StringUtils.stripQuotes(uri);
				if (uri.length() > 0) {
					ITaglibRecord tld = TaglibIndex.resolve(file.getFullPath().toString(), uri, false);
					if (tld == null && fSeverityTaglibUnresolvableURI != NO_SEVERITY) {
						// URI specified but does not resolve
						String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_1, uri);
						LocalizedMessage message = new LocalizedMessage(fSeverityTaglibUnresolvableURI, msgText, file);
						int start = documentRegion.getStartOffset(uriValueRegion);
						int length = uriValueRegion.getTextLength();
						int lineNo = sDoc.getLineOfOffset(start);
						message.setLineNo(lineNo);
						message.setOffset(start);
						message.setLength(length);

						reporter.addMessage(fMessageOriginator, message);
					}
				}
				else if (fSeverityTaglibMissingURI != NO_SEVERITY) {
					// URI specified but empty string
					String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_3, JSP11Namespace.ATTR_NAME_URI);
					LocalizedMessage message = new LocalizedMessage(fSeverityTaglibMissingURI, msgText, file);
					int start = documentRegion.getStartOffset(uriValueRegion);
					int length = uriValueRegion.getTextLength();
					int lineNo = sDoc.getLineOfOffset(start);
					message.setLineNo(lineNo);
					message.setOffset(start);
					message.setLength(length);

					reporter.addMessage(fMessageOriginator, message);
				}
			}
		}
		else if (tagdirValueRegion != null) {
			// URI is specified
			String tagdir = documentRegion.getText(tagdirValueRegion);

			if (file != null) {
				tagdir = StringUtils.stripQuotes(tagdir);
				if (tagdir.length() <= 0 && fSeverityTaglibMissingURI != NO_SEVERITY) {
					// tagdir specified but empty string
					String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_3, JSP20Namespace.ATTR_NAME_TAGDIR);
					LocalizedMessage message = new LocalizedMessage(fSeverityTaglibMissingURI, msgText, file);
					int start = documentRegion.getStartOffset(tagdirValueRegion);
					int length = tagdirValueRegion.getTextLength();
					int lineNo = sDoc.getLineOfOffset(start);
					message.setLineNo(lineNo);
					message.setOffset(start);
					message.setLength(length);

					reporter.addMessage(fMessageOriginator, message);
				}
			}
		}
		else if (fSeverityTaglibMissingURI != NO_SEVERITY) {
			// URI not specified or empty string
			String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_3, JSP11Namespace.ATTR_NAME_URI);
			LocalizedMessage message = new LocalizedMessage(fSeverityTaglibMissingURI, msgText, file);
			int start = documentRegion.getStartOffset();
			int length = documentRegion.getTextLength();
			int lineNo = sDoc.getLineOfOffset(start);
			message.setLineNo(lineNo);
			message.setOffset(start);
			message.setLength(length);

			reporter.addMessage(fMessageOriginator, message);
		}

		prefixValueRegion = getAttributeValueRegion(documentRegion, JSP11Namespace.ATTR_NAME_PREFIX);
		if (prefixValueRegion != null) {
			// prefix specified
			String taglibPrefix = documentRegion.getText(prefixValueRegion);
			taglibPrefix = StringUtils.stripQuotes(taglibPrefix);

			collectTaglibPrefix(documentRegion, prefixValueRegion, taglibPrefix);

			if (isReservedTaglibPrefix(taglibPrefix)) {
				// prefix is a reserved prefix
				String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_0, taglibPrefix);
				int sev = IMessage.HIGH_SEVERITY;
				LocalizedMessage message = (file == null ? new LocalizedMessage(sev, msgText) : new LocalizedMessage(sev, msgText, file));
				int start = documentRegion.getStartOffset(prefixValueRegion);
				int length = prefixValueRegion.getTextLength();
				int lineNo = sDoc.getLineOfOffset(start);
				message.setLineNo(lineNo);
				message.setOffset(start);
				message.setLength(length);

				reporter.addMessage(fMessageOriginator, message);
			}
			if (taglibPrefix.length() == 0 && fSeverityTaglibMissingPrefix != NO_SEVERITY) {
				// prefix is specified but empty
				String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_3, JSP11Namespace.ATTR_NAME_PREFIX);
				LocalizedMessage message = new LocalizedMessage(fSeverityTaglibMissingPrefix, msgText, file);
				int start = documentRegion.getStartOffset(prefixValueRegion);
				int length = prefixValueRegion.getTextLength();
				int lineNo = sDoc.getLineOfOffset(start);
				message.setLineNo(lineNo);
				message.setOffset(start);
				message.setLength(length);

				reporter.addMessage(fMessageOriginator, message);
			}
		}
		else if (fSeverityTaglibMissingPrefix != NO_SEVERITY) {
			// prefix is not specified
			String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_3, JSP11Namespace.ATTR_NAME_PREFIX);
			LocalizedMessage message = new LocalizedMessage(fSeverityTaglibMissingPrefix, msgText, file);
			int start = documentRegion.getStartOffset();
			int length = documentRegion.getTextLength();
			int lineNo = sDoc.getLineOfOffset(start);
			message.setLineNo(lineNo);
			message.setOffset(start);
			message.setLength(length);

			reporter.addMessage(fMessageOriginator, message);
		}
	}

	private void reportTaglibDuplicatePrefixes(IFile file, IReporter reporter, IStructuredDocument document) {
		if (fSeverityTaglibDuplicatePrefixWithDifferentURIs == NO_SEVERITY && fSeverityTaglibDuplicatePrefixWithSameURIs == NO_SEVERITY)
			return;

		String[] prefixes = (String[]) fTaglibPrefixesInUse.keySet().toArray(new String[0]);
		for (int prefixNumber = 0; prefixNumber < prefixes.length; prefixNumber++) {
			int severity = fSeverityTaglibDuplicatePrefixWithSameURIs;

			Object o = fTaglibPrefixesInUse.get(prefixes[prefixNumber]);
			/*
			 * Only care if it's a List (because there was more than one
			 * directive with that prefix) and if we're supposed to report
			 * duplicates
			 */
			if (o instanceof List) {
				List valueRegions = (List) o;
				String uri = null;
				for (int regionNumber = 0; regionNumber < valueRegions.size(); regionNumber++) {
					IStructuredDocumentRegion documentRegion = (IStructuredDocumentRegion) fPrefixValueRegionToDocumentRegionMap.get(valueRegions.get(regionNumber));
					ITextRegion uriValueRegion = getAttributeValueRegion(documentRegion, JSP11Namespace.ATTR_NAME_URI);
					if (uriValueRegion == null) {
						uriValueRegion = getAttributeValueRegion(documentRegion, JSP20Namespace.ATTR_NAME_TAGDIR);
					}
					if (uriValueRegion != null) {
						String uri2 = StringUtils.stripQuotes(documentRegion.getText(uriValueRegion));
						if (uri == null) {
							uri = uri2;
						}
						else {
							if (collator.compare(uri, uri2) != 0) {
								severity = fSeverityTaglibDuplicatePrefixWithDifferentURIs;
							}
						}
					}
				}

				String msgText = NLS.bind(JSPCoreMessages.JSPDirectiveValidator_2, prefixes[prefixNumber]); //$NON-NLS-2$ //$NON-NLS-1$

				// Report an error in all directives using this prefix
				for (int regionNumber = 0; regionNumber < valueRegions.size(); regionNumber++) {

					ITextRegion valueRegion = (ITextRegion) valueRegions.get(regionNumber);
					IStructuredDocumentRegion documentRegion = (IStructuredDocumentRegion) fPrefixValueRegionToDocumentRegionMap.get(valueRegion);
					LocalizedMessage message = (file == null ? new LocalizedMessage(severity, msgText) : new LocalizedMessage(severity, msgText, file));

					// if there's a message, there was an error found
					if (message != null) {
						int start = documentRegion.getStartOffset(valueRegion);
						int length = valueRegion.getTextLength();
						int lineNo = document.getLineOfOffset(start);
						message.setLineNo(lineNo);
						message.setOffset(start);
						message.setLength(length);

						reporter.addMessage(fMessageOriginator, message);
					}
				}
			}
		}
	}

	public void validate(IValidationContext helper, IReporter reporter) throws ValidationException {
		reporter.removeAllMessages(this);
		super.validate(helper, reporter);
	}

	/**
	 * batch validation call
	 */
	protected void validateFile(IFile f, IReporter reporter) {
		if (DEBUG) {
			Logger.log(Logger.INFO, getClass().getName() + " validating: " + f); //$NON-NLS-1$
		}

		IStructuredModel sModel = null;
		try {
			sModel = StructuredModelManager.getModelManager().getModelForRead(f);
			if (sModel != null && !reporter.isCancelled()) {
				performValidation(f, reporter, sModel.getStructuredDocument());
			}
		}
		catch (Exception e) {
			Logger.logException(e);
		}
		finally {
			if (sModel != null)
				sModel.releaseFromRead();
		}
	}
}
