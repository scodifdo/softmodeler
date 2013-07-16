/*******************************************************************************
 * $URL: svn://176.28.48.244/projects/softmodeler/trunk/rcp/plugins/com.softmodeler.ui.rcp/src/com/softmodeler/ui/rcp/LaunchManagerImpl.java $
 *
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui.rcp;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.program.Program;

import com.softmodeler.ui.LaunchManager;

/**
 * @author created by Author: fdo, last update by $Author: fdo $
 * @version $Revision: 18468 $, $Date: 2013-03-11 10:09:44 +0100 (Mon, 11 Mar 2013) $
 */
public class LaunchManagerImpl extends LaunchManager {

	@Override
	public void openExternalBrowser(String url) {
		Program.launch(url);
		// BIG CHANGE
	}

	@Override
	public void browserHistoryBackward(Browser browser) {
		browser.back();
	}

	@Override
	public void browserHistoryForward(Browser browser) {
		browser.forward();
	}

}
