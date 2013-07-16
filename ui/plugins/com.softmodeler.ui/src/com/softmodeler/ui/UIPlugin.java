/*******************************************************************************
 * $URL: svn://176.28.48.244/projects/softmodeler/trunk/ui/plugins/com.softmodeler.ui/src/com/softmodeler/ui/UIPlugin.java $
 * 
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.softmodeler.common.CommonPlugin;
import com.softmodeler.common.util.ModelUtil;
import com.softmodeler.security.IUser;
import com.softmodeler.ui.preferences.ServerPreferenceStore;

/**
 * The activator class controls the plug-in life cycle<br />
 * Handles general UI specific
 * 
 * @author created by Author: fdo, last update by $Author: fdo $
 * @version $Revision: 17826 $, $Date: 2012-12-21 14:01:11 +0100 (Fri, 21 Dec 2012) $
 */
public class UIPlugin extends AbstractUIPlugin {
	/** The plug-in ID */
	public static final String PLUGIN_ID = "com.softmodeler.ui"; //$NON-NLS-1$
	/** The plug-in of used for the ImageDescriptor lookup, can be overridden for own icons */
	public static final String IMAGE_DESCRIPTOR_PLUGIN = PLUGIN_ID;
	/** default icon path */
	public static final String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

	/** Softmodeler UI Logger */
	public static final Logger logger = LoggerFactory.getLogger(PLUGIN_ID);

	/** The shared instance */
	private static UIPlugin plugin;

	/** the application name */
	private static String applicationName;

	/** the local cached server preference stores PluginId=>Store */
	private Map<String, ServerPreferenceStore> preferenceStores;

	/**
	 * The constructor
	 */
	public UIPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);

		preferenceStores = new HashMap<String, ServerPreferenceStore>();

		// initialize model package, prevent communication problems
		ModelUtil.getModelPackages();

		plugin = this;
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
	public static UIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the 'com.softmodeler.ui' plug-in relative path
	 * 
	 * @param symbolicName the symbolic name of the {@link Bundle}.
	 * @param relativePath the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String symbolicName, String relativePath) {
		return UIResourceManager.getInstance().getPluginImageDescriptor(symbolicName, ICONS_PATH + relativePath);
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param relativePath the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String relativePath) {
		return UIResourceManager.getInstance().getPluginImageDescriptor(IMAGE_DESCRIPTOR_PLUGIN, ICONS_PATH + relativePath);
	}

	/**
	 * creates the image found at the path, from the {@link ImageDescriptor}
	 * 
	 * @param relativePath
	 * @return
	 */
	public static Image getImage(String relativePath) {
		ImageDescriptor imageDescriptor = UIResourceManager.getInstance().getPluginImageDescriptor(IMAGE_DESCRIPTOR_PLUGIN,
				ICONS_PATH + relativePath);
		if (imageDescriptor != null) {
			return UIResourceManager.getInstance().getImage(imageDescriptor);
		}
		return null;
	}

	/**
	 * Returns the default server preference store
	 * 
	 * @return
	 */
	public IPreferenceStore getServerPreferenceStore() {
		return getServerPreferenceStore("com.softmodeler.server"); //$NON-NLS-1$
	}

	/**
	 * Returns the server preference store
	 * 
	 * @param pluginId
	 * @return
	 */
	public IPreferenceStore getServerPreferenceStore(String pluginId) {
		if (preferenceStores.containsKey(pluginId)) {
			return preferenceStores.get(pluginId);
		}
		ServerPreferenceStore preferenceStore = new ServerPreferenceStore(pluginId);
		try {
			preferenceStore.load();
		} catch (Exception e) {
			UIUtil.handleException(e);
		}
		preferenceStores.put(pluginId, preferenceStore);
		return preferenceStore;
	}

	/**
	 * Returns the Application name
	 * 
	 * @return
	 */
	public static String getApplicationName() {
		if (applicationName == null) {
			try {
				String varApplicationName = "${com.softmodeler.applicationName}"; //$NON-NLS-1$
				IStringVariableManager variableManager = VariablesPlugin.getDefault().getStringVariableManager();
				applicationName = variableManager.performStringSubstitution(varApplicationName);
				if (applicationName == null) {
					applicationName = "Softmodeler"; //$NON-NLS-1$
				}
			} catch (CoreException e) {
				logger.warn("trouble reading application name", e); //$NON-NLS-1$
			}
		}
		return applicationName;
	}

	public static synchronized Set<Integer> getStoredDomainSelection(String domainSelectionId) {
		IUser user = CommonPlugin.getUserService().getUser();
		if (user.isAnonymous()) {
			return null;
		}

		String selectionKey = "domainSelection." + user.getIndividualId() + ".domain_" + user.getDomain() + "." + domainSelectionId; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		IPreferenceStore preferenceStore = UIPlugin.getDefault().getPreferenceStore();
		String selectionString = preferenceStore.getString(selectionKey);
		Set<Integer> domainSelection = null;
		if (!selectionString.trim().isEmpty()) {
			domainSelection = new HashSet<Integer>();
			String[] splits = selectionString.split(","); //$NON-NLS-1$
			for (String split : splits) {
				Integer domain = new Integer(split);
				domainSelection.add(domain);
			}
		}
		return domainSelection;
	}

	public static synchronized void storeDomainSelection(Set<Integer> domains, String domainSelectionId) {
		IUser user = CommonPlugin.getUserService().getUser();
		if (user.isAnonymous()) {
			return;
		}
		String selectionKey = "domainSelection." + user.getIndividualId() + ".domain_" + user.getDomain() + "." + domainSelectionId; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		IPreferenceStore preferenceStore = UIPlugin.getDefault().getPreferenceStore();
		StringBuilder sb = new StringBuilder();
		for (Integer domain : domains) {
			sb.append(domain.intValue()).append(',');
		}
		sb.setLength(sb.length() - 1);
		preferenceStore.setValue(selectionKey, sb.toString());
	}

}
