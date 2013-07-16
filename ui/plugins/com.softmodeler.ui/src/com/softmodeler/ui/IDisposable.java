/*******************************************************************************
 * $URL: svn://176.28.48.244/projects/softmodeler/trunk/ui/plugins/com.softmodeler.ui/src/com/softmodeler/ui/IDisposable.java $
 *
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui;

/**
 * interface for any class that should be disposed
 * 
 * @author created by Author: fdo, last update by $Author: fdo $
 * @version $Revision: 3346 $, $Date: 2010-03-01 18:27:11 +0100 (Mo, 01 Mrz 2010) $
 */
public interface IDisposable {

	/**
	 * clean up the class/resources
	 */
	void dispose();
}
