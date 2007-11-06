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
package org.eclipse.wst.jsdt.web.core.internal;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.wst.jsdt.web.core.internal.java.search.JsIndexManager;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class JsCorePlugin extends Plugin {
	// The shared instance.
	private static JsCorePlugin plugin;
	public static final String PLUGIN_ID = "org.eclipse.wst.jsdt.web.core"; //$NON-NLS-1$
	
	/**
	 * Returns the shared instance.
	 * 
	 * @deprecated - will be removed. Currently used to get "model preferences",
	 *             but there are other, better ways.
	 */

	public static JsCorePlugin getDefault() {
		return JsCorePlugin.plugin;
	}
	
	/**
	 * The constructor.
	 */
	public JsCorePlugin() {
		super();
		JsCorePlugin.plugin = this;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	
	public void start(BundleContext context) throws Exception {
		super.start(context);
		// JSPIndexManager depends on TaglibController, so TaglibController
		// should be started first
		// listen for classpath changes
		JsIndexManager.getInstance().initialize();
		// listen for resource changes to update content properties keys
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	
	public void stop(BundleContext context) throws Exception {
		// stop listenning for resource changes to update content properties
		// keys
		// stop any indexing
		JsIndexManager.getInstance().shutdown();
		super.stop(context);
	}
}
