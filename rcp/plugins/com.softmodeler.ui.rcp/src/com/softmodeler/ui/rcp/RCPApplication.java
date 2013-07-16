/*******************************************************************************
 * $URL: svn://176.28.48.244/projects/softmodeler/trunk/rcp/plugins/com.softmodeler.ui.rcp/src/com/softmodeler/ui/rcp/RCPApplication.java $
 * 
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui.rcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.softmodeler.client.Client;
import com.softmodeler.common.CommonPlugin;
import com.softmodeler.common.domain.DomainUtil;
import com.softmodeler.common.util.LocaleUtil;
import com.softmodeler.common.util.PathUtil;
import com.softmodeler.common.util.WebUrlUtil;
import com.softmodeler.security.AuthenticationException;
import com.softmodeler.security.ISession;
import com.softmodeler.security.IUser;
import com.softmodeler.security.SecurityConstants;
import com.softmodeler.security.SessionUtil;
import com.softmodeler.ui.UIUtil;
import com.softmodeler.ui.app.ClientApplication;
import com.softmodeler.ui.rcp.dialogs.ApplicationEntryDialog;
import com.softmodeler.ui.rcp.dialogs.SessionCookieSettingDialog;
import com.softmodeler.ui.rcp.internal.RCPMessages;
import com.softmodeler.ui.rcp.update.AutomaticUpdater;
import com.softmodeler.windows.WindowsUtil;

/**
 * @author created by Author: phe, last update by $Author: fdo $
 * @version $Revision: 19165 $, $Date: 2013-07-03 20:25:21 +0200 (Wed, 03 Jul 2013) $
 */
public abstract class RCPApplication extends ClientApplication {
	/** softmodeler argument prefix */
	private static final String ARGUMENT_PREFIX = "softmodeler."; //$NON-NLS-1$
	/** the server argument key */
	private static final String SERVER_ARGUMENT_KEY = ARGUMENT_PREFIX + "server"; //$NON-NLS-1$
	/** the username argument key */
	private static final String USERNAME_ARGUMENT_KEY = ARGUMENT_PREFIX + "username"; //$NON-NLS-1$
	/** the password argument key */
	private static final String PASSWORD_ARGUMENT_KEY = ARGUMENT_PREFIX + "password"; //$NON-NLS-1$
	/** the session id argument key */
	private static final String SESSIONID_ARGUMENT_KEY = ARGUMENT_PREFIX + "session"; //$NON-NLS-1$
	/** the domain id argument key */
	public static final String DOMAIN_ARGUMENT_KEY = ARGUMENT_PREFIX + "domain"; //$NON-NLS-1$
	/** the update site argument key */
	private static final String UPDATE_SITE_ARGUMENT_KEY = ARGUMENT_PREFIX + "updatesite"; //$NON-NLS-1$
	/** the start with login argument key */
	private static final String NEW_LOGIN_REQUIRED_ARGUMENT_KEY = ARGUMENT_PREFIX + SecurityConstants.NEW_LOGIN_REQUIRED; //;

	/** default server URL */
	private static final String DEFAULT_SERVER_URL = "http://127.0.0.1/"; //$NON-NLS-1$

	/** update site name */
	private static final String UPDATE_SITE = "site"; //$NON-NLS-1$

	/**
	 * constructor
	 */
	public RCPApplication() {
		Client.getDefault(); // call to make sure that the client plug-in is loaded
	}

	/**
	 * start the application
	 * 
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public Object start(IApplicationContext context) throws Exception {
		printStartupMessage();

		Display display = PlatformUI.createDisplay();
		Shell shell = new Shell(display);

		// read the update site, if not specified http://serverUrl/site/ is taken  
		String updateSite = getArgument(UPDATE_SITE_ARGUMENT_KEY);
		if (updateSite == null || updateSite.trim().isEmpty()) {
			updateSite = internalGetDataServerUrl() + UPDATE_SITE + PathUtil.SEP;
		}

		// check for updates
		AutomaticUpdater updater = new AutomaticUpdater();
		updater.checkForUpdates(updateSite);

		try {
			WindowsUtil.getDefault().register();
		} catch (Exception e1) {
			UIUtil.handleException(e1);
			return IApplication.EXIT_OK;
		}

		while (true) {
			try {
				String dataServerId = initialize();
				// set id of the data server
				setDataServerId(dataServerId);
				break;
			} catch (Exception e) {
				UIUtil.handleException(e, shell);

				ConfigurationDialog dialog = new ConfigurationDialog(shell, updateSite);
				if (dialog.open() == Window.OK) {
					return IApplication.EXIT_RESTART;
				}
				return IApplication.EXIT_OK;
			}
		}

		try {
			AuthenticationResult authenticationResult = authenticate(shell);
			if (authenticationResult == AuthenticationResult.SUCCESS) {
				printStartupFinishedMessage();
				int returnCode = PlatformUI.createAndRunWorkbench(display, getApplicationWorkbenchAdvisor());
				if (returnCode == PlatformUI.RETURN_RESTART) {
					ISession currentSession = CommonPlugin.getUserService().getSession();
					if (currentSession != null) {
						Object value = currentSession.getValue(SESSION_KEY_EXITCODE);
						if (SESSION_VALUE_EXITRELAUNCH.equals(value) || SESSION_VALUE_EXITRELAUNCH_FOR_NEW_LOGIN.equals(value)) {
							logger.info("Exit with EXIT_RELAUNCH"); //$NON-NLS-1$
							return IApplication.EXIT_RELAUNCH;
						}
					}
					logger.info("Exit with EXIT_RESTART"); //$NON-NLS-1$
					return IApplication.EXIT_RESTART;
				}
			} else if (authenticationResult == AuthenticationResult.RESTART) {
				logger.info("Exit with EXIT_RELAUNCH"); //$NON-NLS-1$
				return IApplication.EXIT_RELAUNCH;
			}
			return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}
	}

	/**
	 * registers the remote services and returns the application id of the server
	 * 
	 * @return application id of the server
	 * @throws Exception
	 */
	protected String initialize() throws Exception {
		setDataServerUrl(internalGetDataServerUrl());

		try {
			String dataServerId = Client.getDefault().registerServices(getDataServerUrl());

			SessionUtil.setAppId(dataServerId);
			SessionUtil.getServerSessionCookieName(); // must initialize the server session cookie name here before other calls happen
			return dataServerId;
		} catch (Exception e) {
			logger.error("error in initialize", e); //$NON-NLS-1$
			throw new IllegalStateException(NLS.bind(RCPMessages.get().RCPApplication_ConnectionError, getDataServerUrl()), e);
		}
	}

	@Override
	protected String internalGetDataServerUrl() throws Exception {
		String serverUrl = getArgument(SERVER_ARGUMENT_KEY);
		if (serverUrl == null) {
			serverUrl = DEFAULT_SERVER_URL;
		}
		if (!serverUrl.endsWith(PathUtil.SEP)) {
			serverUrl = serverUrl + PathUtil.SEP;
		}
		return serverUrl;
	}

	/**
	 * Provides a dialog for authentication. If authentication succeeds, the corresponding tokens are set.
	 * 
	 * @return whether authentication succeeded
	 */
	@Override
	protected AuthenticationResult authenticate(Shell shell) {
		AuthenticationResult result = AuthenticationResult.INITIAL;
		logger.info("Authentication"); //$NON-NLS-1$

		String newLoginRequired = getArgument(NEW_LOGIN_REQUIRED_ARGUMENT_KEY);
		if (!Boolean.TRUE.toString().equalsIgnoreCase(newLoginRequired)) {
			result = authenticateWithArguments(shell);
		} else {
			logger.info("Omitting authenticateWithArguments because softmodeler.newLoginRequired is true"); //$NON-NLS-1$
		}
		if (result != AuthenticationResult.SUCCESS) {
			result = authenticateWithDialog(shell);
		} else {
			synchronizeBrowserDataSessionCookie(shell);
		}

		if (result == AuthenticationResult.SUCCESS) {
			result = doRelaunchIfNecessary();
			if (result == AuthenticationResult.SUCCESS) {
				String domainArgument = getArgument(DOMAIN_ARGUMENT_KEY);
				if (domainArgument != null) {
					int requestedDomain = Integer.parseInt(domainArgument);
					if (CommonPlugin.getUserService().getDomain() != requestedDomain) {
						// If the domain of the authenticated user is not the requested one, 
						// we must switch the domain and update the session with the modified user
						try {
							setCurrentUser(DomainUtil.switchDomain(requestedDomain));
						} catch (Exception e) {
							UIUtil.handleException(e);
							result = AuthenticationResult.FAILED;
						}
					}
				}
			}
		}

		logger.info("--------------------------------------------------------"); //$NON-NLS-1$
		return result;
	}

	/**
	 * stop the RCP application
	 */
	public void stop() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null) {
			return;
		}
		final Display display = workbench.getDisplay();
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				if (!display.isDisposed()) {
					workbench.close();
				}
			}
		});
	}

	/**
	 * Prepare a relaunch using the login information to avoid a reappearing login dialog.
	 */
	public static void prepareRelaunch() {
		logger.info("Relaunching Application"); //$NON-NLS-1$
		logger.info("--- setting relaunch parameters"); //$NON-NLS-1$

		ISession currentSession = CommonPlugin.getUserService().getSession();
		if (currentSession != null) {
			currentSession.setValue(SESSION_KEY_EXITCODE, SESSION_VALUE_EXITRELAUNCH);
		}

		// set the eclipse relaunch property
		StringBuffer arguments = new StringBuffer();
		arguments.append("${eclipse.vm}\n"); //$NON-NLS-1$
		arguments.append("-nl\n").append(CommonPlugin.getUserService().getLocaleLanguage()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$

		arguments.append("-vmargs\n"); //$NON-NLS-1$

		String vmargs = System.getProperty("eclipse.vmargs"); //$NON-NLS-1$
		if (vmargs != null) {
			// remove existing arguments that may conflict
			vmargs = removeVMArgument(vmargs, "-D" + SESSIONID_ARGUMENT_KEY); //$NON-NLS-1$
			vmargs = removeVMArgument(vmargs, "-D" + NEW_LOGIN_REQUIRED_ARGUMENT_KEY); //$NON-NLS-1$
			vmargs = removeVMArgument(vmargs, "-D" + DOMAIN_ARGUMENT_KEY); //$NON-NLS-1$

			int beginIndex = vmargs.indexOf("-Djava.class.path"); //$NON-NLS-1$
			int endIndex = vmargs.indexOf("\n", beginIndex); //$NON-NLS-1$
			String args = vmargs.substring(0, beginIndex);
			args += vmargs.substring(endIndex + 1);
			arguments.append(args);
		} else {
			arguments.append("-D").append(SERVER_ARGUMENT_KEY).append("=").append(getDataServerUrl().toString()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		arguments.append("-D").append(SESSIONID_ARGUMENT_KEY).append("=").append(CommonPlugin.getUserService().getSessionId()); //$NON-NLS-1$ //$NON-NLS-2$
		String domain = System.getProperty(DOMAIN_ARGUMENT_KEY);
		if (domain != null) {
			arguments.append("\n-D").append(DOMAIN_ARGUMENT_KEY).append("=").append(domain).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		logger.info("--- arguments:\n{}", arguments); //$NON-NLS-1$
		System.setProperty("eclipse.exitcode", Integer.toString(IApplication.EXIT_RELAUNCH)); //$NON-NLS-1$
		System.getProperties().setProperty(IApplicationContext.EXIT_DATA_PROPERTY, arguments.toString());
	}

	private static String removeVMArgument(String eclipseArgumentsString, String argumentKey) {
		StringBuilder arguments = new StringBuilder(eclipseArgumentsString);
		int argumentPosition = arguments.indexOf(argumentKey);
		logger.debug("trying to remove argmument " + argumentKey + " from arguments: \n" + arguments); //$NON-NLS-1$ //$NON-NLS-2$

		if (argumentPosition > -1) {
			logger.debug("removing argmument " + argumentKey + " at position " + argumentPosition + " from arguments: \n" + arguments); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			int endPosition = arguments.indexOf("\n", argumentPosition); //$NON-NLS-1$
			logger.debug("endposition is: {0}", endPosition); //$NON-NLS-1$
			arguments.replace(argumentPosition, endPosition + 1, ""); //$NON-NLS-1$
			logger.debug("arguments after replacing: \n {0}", arguments); //$NON-NLS-1$
		}
		return arguments.toString();
	}

	/**
	 * Do a relaunch that explicitly requires a new login
	 */
	public static void restartWithNewLogin() {
		logger.info("Restarting Application with new login"); //$NON-NLS-1$
		logger.info("--- setting relaunch parameters"); //$NON-NLS-1$

		ISession currentSession = CommonPlugin.getUserService().getSession();
		if (currentSession != null) {
			currentSession.setValue(SESSION_KEY_EXITCODE, SESSION_VALUE_EXITRELAUNCH_FOR_NEW_LOGIN);
		}

		// set the eclipse relaunch property
		StringBuffer arguments = new StringBuffer();
		arguments.append("${eclipse.vm}\n"); //$NON-NLS-1$
		arguments.append("-nl\n").append(LocaleUtil.getDefaultLanguage()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$

		arguments.append("-vmargs\n"); //$NON-NLS-1$

		String vmargs = System.getProperty("eclipse.vmargs"); //$NON-NLS-1$
		if (vmargs != null) {
			// remove existing arguments that may conflict
			vmargs = removeVMArgument(vmargs, "-D" + SESSIONID_ARGUMENT_KEY); //$NON-NLS-1$
			vmargs = removeVMArgument(vmargs, "-D" + NEW_LOGIN_REQUIRED_ARGUMENT_KEY); //$NON-NLS-1$
			vmargs = removeVMArgument(vmargs, "-D" + DOMAIN_ARGUMENT_KEY); //$NON-NLS-1$

			int beginIndex = vmargs.indexOf("-Djava.class.path"); //$NON-NLS-1$
			int endIndex = vmargs.indexOf("\n", beginIndex); //$NON-NLS-1$
			String args = vmargs.substring(0, beginIndex);
			args += vmargs.substring(endIndex + 1);
			arguments.append(args);
		} else {
			arguments.append("-D").append(SERVER_ARGUMENT_KEY).append("=").append(getDataServerUrl().toString()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		arguments.append("-D").append(NEW_LOGIN_REQUIRED_ARGUMENT_KEY).append("=true"); //$NON-NLS-1$ //$NON-NLS-2$

		logger.info("--- arguments:\n{}", arguments); //$NON-NLS-1$
		System.setProperty("eclipse.exitcode", Integer.toString(IApplication.EXIT_RELAUNCH)); //$NON-NLS-1$
		System.getProperties().setProperty(IApplicationContext.EXIT_DATA_PROPERTY, arguments.toString());
		PlatformUI.getWorkbench().restart();
	}

	protected AuthenticationResult doRelaunchIfNecessary() {
		if (checkForRelaunch()) {
			prepareRelaunch();
			return AuthenticationResult.RESTART;
		}
		return AuthenticationResult.SUCCESS;
	}

	/**
	 * checks if relaunch is required
	 * 
	 * @return whether a locale switch has to be performed
	 */
	private boolean checkForRelaunch() {
		String defaultLanguage = Locale.getDefault().getLanguage();
		String userLanguage = CommonPlugin.getUserService().getLocaleLanguage();
		boolean isRelaunchNecessary = !defaultLanguage.equals(userLanguage);
		if (isRelaunchNecessary) {
			logger.info("--- locale mismatch detected: default={}, user={}", defaultLanguage, userLanguage); //$NON-NLS-1$
		}
		return isRelaunchNecessary;
	}

	/**
	 * Prints the startup information.
	 */
	private void printStartupMessage() {
		logger.info("--------------------------------------------------------"); //$NON-NLS-1$
		logger.info("Starting Application"); //$NON-NLS-1$
		logger.info("--------------------------------------------------------"); //$NON-NLS-1$
		logger.info("Language"); //$NON-NLS-1$
		logger.info("--- {}", Locale.getDefault().getDisplayLanguage()); //$NON-NLS-1$
		logger.info("--------------------------------------------------------"); //$NON-NLS-1$
		logger.info("Client                                                  "); //$NON-NLS-1$
		logger.info("--- version: unknown"); //$NON-NLS-1$
		logger.info("--------------------------------------------------------"); //$NON-NLS-1$
		logger.info("Server                                                  "); //$NON-NLS-1$
		logger.info("--- version: unknown                                    "); //$NON-NLS-1$
		logger.info("--- build date: unknown                                 "); //$NON-NLS-1$
		logger.info("--- milestone: unknown                                  "); //$NON-NLS-1$
		logger.info("--------------------------------------------------------"); //$NON-NLS-1$
		if (logger.isDebugEnabled()) {
			logger.info("System arguments:"); //$NON-NLS-1$

			Set<Object> keySet = System.getProperties().keySet();
			TreeSet<String> keys = new TreeSet<String>();
			for (Object object : keySet) {
				keys.add((String) object);
			}
			for (String key : keys) {
				logger.info(key + " = " + System.getProperty(key)); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Prints the startup information.
	 */
	private void printStartupFinishedMessage() {
		logger.info("Application Started"); //$NON-NLS-1$
		logger.info("--------------------------------------------------------"); //$NON-NLS-1$
	}

	/**
	 * Proceeds an authentication without using the Login dialog.
	 * 
	 * @param shell The {@link Shell} to use
	 * @return The result after authentication with dialog
	 */
	protected AuthenticationResult authenticateWithArguments(Shell shell) {
		// check arguments for auto login
		String sessionid = getArgument(SESSIONID_ARGUMENT_KEY);
		if (sessionid != null) {
			logger.info("--- try to reconnect session {}", sessionid); //$NON-NLS-1$
			IUser user = tryToReconnect(sessionid);
			logger.info("--- user after reconnect {}", user); //$NON-NLS-1$
			if (user != null) {
				return AuthenticationResult.SUCCESS;
			}
			System.clearProperty(SESSIONID_ARGUMENT_KEY);
		}
		String username = getArgument(USERNAME_ARGUMENT_KEY);
		String password = getArgument(PASSWORD_ARGUMENT_KEY);
		if (username != null && password != null) {
			logger.info("--- program arguments found for automatic authentication"); //$NON-NLS-1$
			logger.info("--- authenticating user: {}", username); //$NON-NLS-1$
			return doAuthenticate(shell, username, password);
		}
		return AuthenticationResult.INITIAL;
	}

	/**
	 * Proceeds an authentication using the Login dialog.
	 * 
	 * @param shell The {@link Shell} to use
	 * @return The result after authentication with dialog
	 */

	protected AuthenticationResult authenticateWithDialog(Shell shell) {
		ApplicationEntryDialog entryShell = new ApplicationEntryDialog(shell, getApplicationId());
		String startpageUrlWithApplicationId = PathUtil.appendParam(getStartPageUrl(), "applicationId", getApplicationId()); //$NON-NLS-1$

		String newLoginRequired = getArgument(NEW_LOGIN_REQUIRED_ARGUMENT_KEY);
		if (Boolean.TRUE.toString().equalsIgnoreCase(newLoginRequired)) {
			startpageUrlWithApplicationId = PathUtil.appendParam(getStartPageUrl(), SecurityConstants.NEW_LOGIN_REQUIRED, "true"); //$NON-NLS-1$
			System.clearProperty(NEW_LOGIN_REQUIRED_ARGUMENT_KEY); // remove system argument so that it is not included if later a relaunch is needed
		}

		IUser user = entryShell.open(startpageUrlWithApplicationId, SessionUtil.getServerSessionCookieName(), getDataServerUrl());
		if (user != null) {
			return AuthenticationResult.SUCCESS;
		}
		return AuthenticationResult.FAILED;
	}

	/**
	 * authenticate/login the user
	 * 
	 * @param username
	 * @param password
	 * @param applicationId
	 * @return
	 * @throws AuthenticationException
	 * @throws Exception
	 */
	@Override
	public synchronized IUser authenticate(String username, String password, String applicationId) throws AuthenticationException,
			Exception {
		Client.username = username; // temporary hack for login
		Client.password = password; // temporary hack for login
		Client.applicationid = applicationId; // temporary hack for login

		return super.authenticate(username, password, applicationId);
	}

	/**
	 * Clears all present data session cookies from the browser and sets the cookie of the current data session. This
	 * method will only return 'true' if the data session cookie could be set.
	 */
	public boolean synchronizeBrowserDataSessionCookie(Shell shell) {
		String dataSessionId = CommonPlugin.getUserService().getSessionId();
		SessionCookieSettingDialog dlg = null;
		boolean returnValue = false;
		boolean retry = true;
		while (retry) {
			dlg = new SessionCookieSettingDialog(shell, SessionUtil.getServerSessionCookieName(), dataSessionId, WebUrlUtil.getWebBaseUrl()
					+ "/refreshCookie/" + dataSessionId); //$NON-NLS-1$
			dlg.open();
			if (dlg.isCookieSet()) {
				returnValue = true;
				retry = false;
			} else {
				boolean respose = MessageDialog.openQuestion(shell, RCPMessages.get().RCPApplication_ConfigurationProblem,
						RCPMessages.get().RCPApplication_ConfigurationProblemDescription);
				if (!respose) {
					retry = false;
				}
			}
			dlg.dispose();
		}
		dlg = null;
		return returnValue;
	}

	/**
	 * returns a system argument according to the passed key
	 * 
	 * @param key
	 * @return
	 */
	protected static String getArgument(String key) {
		return System.getProperty(key);
	}

	/**
	 * dialog that configures the required values for a valid connection
	 * 
	 * @author created by Author: fdo, last update by $Author: fdo $
	 * @version $Revision: 19165 $, $Date: 2013-07-03 20:25:21 +0200 (Wed, 03 Jul 2013) $
	 */
	protected static class ConfigurationDialog extends FormDialog {
		/** update button id */
		protected static int UPDATE_ID = 99;

		/** server URL text field */
		private Text urlText;
		/** proxy host text field */
		private Text proxyHostText;
		/** proxy port text field */
		private Text proxyPortText;

		/** update site URL */
		private String updateSite;

		/**
		 * constructor
		 * 
		 * @param shell
		 * @param updateSite update site URL
		 */
		public ConfigurationDialog(Shell shell, String updateSite) {
			super(shell);
			this.updateSite = updateSite;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(340, 180);
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);

			createButton(parent, UPDATE_ID, RCPMessages.get().RCPApplication_UpdateButton, false);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			getShell().setText(RCPMessages.get().RCPApplication_ChangeServerUrl);

			FormToolkit toolkit = mform.getToolkit();
			Composite body = mform.getForm().getBody();
			body.setLayout(new GridLayout(2, false));

			toolkit.createLabel(body, RCPMessages.get().RCPApplication_ServerURL);
			urlText = toolkit.createText(body, getArgument(SERVER_ARGUMENT_KEY), SWT.BORDER);
			urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			toolkit.createLabel(body, RCPMessages.get().RCPApplication_ProxyHost);
			proxyHostText = toolkit.createText(body, getArgument("http.proxyHost"), SWT.BORDER); //$NON-NLS-1$
			proxyHostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			toolkit.createLabel(body, RCPMessages.get().RCPApplication_ProxyPort);
			proxyPortText = toolkit.createText(body, getArgument("http.proxyPort"), SWT.BORDER); //$NON-NLS-1$
			proxyPortText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		}

		@Override
		protected void buttonPressed(int buttonId) {
			if (buttonId == UPDATE_ID) {
				AutomaticUpdater updater = new AutomaticUpdater();
				updater.checkForUpdates(updateSite);
			}
			super.buttonPressed(buttonId);
		}

		@Override
		protected void okPressed() {
			try {
				String installArea = System.getProperty("osgi.install.area"); //$NON-NLS-1$
				String launcherName = System.getProperty("eclipse.launcher.name"); //$NON-NLS-1$

				URL url = PathUtil.convertToURL(installArea + launcherName + ".ini"); //$NON-NLS-1$
				File file = PathUtil.convertToFile(url);

				logger.info("read ini file {}", url); //$NON-NLS-1$

				if (!file.canRead()) {
					MessageDialog.openError(getShell(), RCPMessages.get().RCPApplication_ConfirurationError,
							RCPMessages.get().RCPApplication_ReadError + url);
					return;
				}
				if (!file.canWrite()) {
					MessageDialog.openError(getShell(), RCPMessages.get().RCPApplication_ConfirurationError,
							RCPMessages.get().RCPApplication_WriteError + url);
					return;
				}

				StringBuilder str = readFile(file);

				// handle URL argument
				String serverUrl = urlText.getText();
				if (!serverUrl.trim().isEmpty()) {
					setArgument(SERVER_ARGUMENT_KEY, serverUrl, str);
				}

				// handle proxy settings
				String proxyHost = proxyHostText.getText();
				String proxyPort = proxyPortText.getText();

				if (!proxyHost.trim().isEmpty()) {
					setArgument("http.proxyHost", proxyHost, str); //$NON-NLS-1$
					setArgument("proxyHost", proxyHost, str); //$NON-NLS-1$
					setArgument("proxySet", Boolean.TRUE.toString(), str); //$NON-NLS-1$
					if (!proxyPort.trim().isEmpty()) {
						setArgument("http.proxyPort", proxyPort, str); //$NON-NLS-1$
						setArgument("proxyPort", proxyPort, str); //$NON-NLS-1$
					}
				}

				FileWriter writer = new FileWriter(file);
				writer.write(str.toString());
				writer.close();
			} catch (Exception e) {
				UIUtil.handleException(e);
			}
			super.okPressed();
		}

		/**
		 * sets the argument, replaces existing ones appends new ones
		 * 
		 * @param key
		 * @param value
		 * @param str
		 */
		private void setArgument(String key, String value, StringBuilder str) {
			key = "-D" + key + "="; //$NON-NLS-1$ //$NON-NLS-2$
			int index = str.indexOf(key);
			String line = key + value;
			if (index != -1) {
				str.replace(index, str.indexOf("\n", index), line); //$NON-NLS-1$
			} else {
				str.append(line).append("\n"); //$NON-NLS-1$
			}
		}

		/**
		 * reads the product INI file and returns the content in a string builder
		 * 
		 * @param file INI file
		 * @throws FileNotFoundException
		 * @throws IOException
		 */
		private StringBuilder readFile(File file) throws FileNotFoundException, IOException {
			StringBuilder str = new StringBuilder();
			FileReader reader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(reader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				str.append(line).append("\n"); //$NON-NLS-1$
			}
			bufferedReader.close();
			return str;
		}
	}
}
