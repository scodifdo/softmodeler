/*******************************************************************************
 * $URL: svn://176.28.48.244/projects/softmodeler/trunk/ui/plugins/com.softmodeler.ui/src/com/softmodeler/ui/LaunchManager.java $
 *
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui;

import org.eclipse.swt.browser.Browser;

import com.softmodeler.common.util.RuntimeUtil;

/**
 * @author created by Author: fdo, last update by $Author: fdo $
 * @version $Revision: 18468 $, $Date: 2013-03-11 10:09:44 +0100 (Mon, 11 Mar 2013) $
 */
public abstract class LaunchManager {
	/** singleton instance */
	private static LaunchManager instance;

	/**
	 * singleton constructor
	 */
	protected LaunchManager() {
	}

	/**
	 * Returns the singleton instance, depending on the runtime (RCP/RAP)
	 * 
	 * @return
	 */
	public static LaunchManager getInstance() {
		if (instance == null) {
			instance = RuntimeUtil.newInstance(LaunchManager.class);
		}
		return instance;
	}

	/**
	 * open the passed URL in an external browser
	 * 
	 * @param url
	 */
	public abstract void openExternalBrowser(String url);

	/**
	 * move the browser history back, environment specific
	 * 
	 * @param browser
	 */
	public abstract void browserHistoryBackward(Browser browser);

	/**
	 * move the browser history forward, environment specific
	 * 
	 * @param browser
	 */
	public abstract void browserHistoryForward(Browser browser);
}
