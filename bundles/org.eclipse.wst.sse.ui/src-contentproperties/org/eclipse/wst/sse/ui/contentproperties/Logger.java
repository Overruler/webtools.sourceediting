/*******************************************************************************
 * Copyright (c) 2001, 2004 IBM Corporation and others.
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
package org.eclipse.wst.sse.ui.contentproperties;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;

/**
 * @deprecated This plugin has combined with the org.eclipse.wst.sse.ui plugin.
 *             Use SSEUIPlugin instead.
 */
public class Logger {
	public static final int ERROR = IStatus.ERROR; // 4
	public static final int ERROR_DEBUG = 200 + ERROR;
	private static Plugin fPlugin = ContentPropertiesPlugin.getDefault();
	private static final String fPluginId = fPlugin.getDescriptor().getUniqueIdentifier();
	public static final int INFO = IStatus.INFO; // 1
	public static final int INFO_DEBUG = 200 + INFO;

	public static final int OK = IStatus.OK; // 0

	public static final int OK_DEBUG = 200 + OK;

	private static final String TRACEFILTER_LOCATION = "/debug/tracefilter"; //$NON-NLS-1$
	public static final int WARNING = IStatus.WARNING; // 2
	public static final int WARNING_DEBUG = 200 + WARNING;

	/**
	 * Adds message to log.
	 * 
	 * @param level
	 *            severity level of the message (OK, INFO, WARNING, ERROR,
	 *            OK_DEBUG, INFO_DEBUG, WARNING_DEBUG, ERROR_DEBUG)
	 * @param message
	 *            text to add to the log
	 * @param exception
	 *            exception thrown
	 */
	protected static void _log(int level, String message, Throwable exception) {
		if (level == OK_DEBUG || level == INFO_DEBUG || level == WARNING_DEBUG || level == ERROR_DEBUG) {
			if (!isDebugging())
				return;
		}

		int severity = IStatus.OK;
		switch (level) {
			case INFO_DEBUG :
			case INFO :
				severity = IStatus.INFO;
				break;
			case WARNING_DEBUG :
			case WARNING :
				severity = IStatus.WARNING;
				break;
			case ERROR_DEBUG :
			case ERROR :
				severity = IStatus.ERROR;
		}
		message = (message != null) ? message : "null"; //$NON-NLS-1$
		Status statusObj = new Status(severity, fPluginId, severity, message, exception);
		fPlugin.getLog().log(statusObj);
	}

	/**
	 * Prints message to log if category matches /debug/tracefilter option.
	 * 
	 * @param message
	 *            text to print
	 * @param category
	 *            category of the message, to be compared with
	 *            /debug/tracefilter
	 */
	protected static void _trace(String category, String message, Throwable exception) {
		if (isTracing(category)) {
			message = (message != null) ? message : "null"; //$NON-NLS-1$
			Status statusObj = new Status(IStatus.OK, fPluginId, IStatus.OK, message, exception);
			fPlugin.getLog().log(statusObj);
		}
	}

	/**
	 * @return true if the plugin for this logger is debugging
	 */
	public static boolean isDebugging() {
		return fPlugin.isDebugging();
	}

	/**
	 * Determines if currently tracing a category
	 * 
	 * @param category
	 * @return true if tracing category, false otherwise
	 */
	public static boolean isTracing(String category) {
		if (!isDebugging())
			return false;

		String traceFilter = Platform.getDebugOption(fPluginId + TRACEFILTER_LOCATION);
		if (traceFilter != null) {
			StringTokenizer tokenizer = new StringTokenizer(traceFilter, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String cat = tokenizer.nextToken().trim();
				if (category.equals(cat)) {
					return true;
				}
			}
		}
		return false;
	}

	public static void log(int level, String message) {
		_log(level, message, null);
	}

	public static void log(int level, String message, Throwable exception) {
		_log(level, message, exception);
	}

	public static void logException(String message, Throwable exception) {
		_log(ERROR, message, exception);
	}

	public static void logException(Throwable exception) {
		_log(ERROR, exception.getMessage(), exception);
	}

	public static void trace(String category, String message) {
		_trace(category, message, null);
	}

	public static void traceException(String category, String message, Throwable exception) {
		_trace(category, message, exception);
	}

	public static void traceException(String category, Throwable exception) {
		_trace(category, exception.getMessage(), exception);
	}
}
