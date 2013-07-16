/*******************************************************************************
 * $URL: svn://176.28.48.244/projects/softmodeler/trunk/ui/plugins/com.softmodeler.ui/src/com/softmodeler/ui/UIException.java $
 * 
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui;

import com.softmodeler.common.nls.NLS;

/**
 * Represents an exception thrown during UI generation
 * 
 * @author created by Author: fdo, last update by $Author: fdo $
 * @version $Revision: 8633 $, $Date: 2010-12-16 11:52:49 +0100 (Do, 16 Dez 2010) $
 */
public class UIException extends RuntimeException {
	private static final long serialVersionUID = -8827908171472230073L;

	public UIException() {
		super();
	}

	public UIException(Throwable cause) {
		super(cause);
	}

	public UIException(String message) {
		super(message);
	}

	public UIException(String message, Throwable cause) {
		super(message, cause);
	}

	public UIException(String message, Object... objects) {
		super(NLS.bind(message, objects));
	}

	public UIException(String message, Throwable cause, Object... objects) {
		super(NLS.bind(message, objects), cause);
	}
}
