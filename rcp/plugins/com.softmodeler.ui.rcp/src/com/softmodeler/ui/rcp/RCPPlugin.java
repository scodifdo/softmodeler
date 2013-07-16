/*******************************************************************************
 * $URL: $
 * 
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui.rcp;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.service.localization.LocaleProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmodeler.security.ISessionHolder;
import com.softmodeler.security.SimpleSessionHolder;

/**
 * @author created by Author: fdo, last update by $Author: $
 * @version $Revision: $, $Date: $
 */
public class RCPPlugin extends AbstractUIPlugin {
	/** The plug-in ID */
	public static final String PLUGIN_ID = "com.softmodeler.ui.rcp"; //$NON-NLS-1$
	/** icon path */
	private static final String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

	/** Softmodeler RCP UI Logger */
	public static final Logger logger = LoggerFactory.getLogger(PLUGIN_ID);

	/** The shared instance */
	private static RCPPlugin plugin;

	/**
	 * The constructor
	 */
	public RCPPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		String[] servicenames = new String[] { ISessionHolder.class.getName(), LocaleProvider.class.getName() };
		context.registerService(servicenames, new SimpleSessionHolder(), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static RCPPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param relativePath the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String relativePath) {
		return imageDescriptorFromPlugin(PLUGIN_ID, ICONS_PATH + relativePath);
	}
}
