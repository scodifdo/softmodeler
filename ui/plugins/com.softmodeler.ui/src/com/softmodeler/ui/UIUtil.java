/*******************************************************************************
 * $URL: svn://176.28.48.244/projects/softmodeler/trunk/ui/plugins/com.softmodeler.ui/src/com/softmodeler/ui/UIUtil.java $
 * 
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPart3;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

import com.softmodeler.common.CommonPlugin;
import com.softmodeler.common.ServerException;
import com.softmodeler.common.ValidationException;
import com.softmodeler.common.helpers.TreeChangeTracker;
import com.softmodeler.common.nls.LabelStorage;
import com.softmodeler.common.nls.MessagesLoader;
import com.softmodeler.common.typeinfo.TypeInfos;
import com.softmodeler.common.util.ExtensionPointUtil;
import com.softmodeler.common.util.IdUtil;
import com.softmodeler.common.util.LocaleUtil;
import com.softmodeler.common.util.ModelUtil;
import com.softmodeler.common.util.PathUtil;
import com.softmodeler.common.util.ResourceUtil;
import com.softmodeler.common.util.model.ExtensionModelUtil;
import com.softmodeler.model.BasicObject;
import com.softmodeler.model.ObjectRef;
import com.softmodeler.model.TreeNode;
import com.softmodeler.model.TreeNodeChild;
import com.softmodeler.security.IAccessDecision;
import com.softmodeler.service.IAccessControlService;
import com.softmodeler.service.IModificationInfoService;
import com.softmodeler.ui.comp.UIDefinition;
import com.softmodeler.ui.comp.UIFactory;
import com.softmodeler.ui.dialogs.BrowserWindow;
import com.softmodeler.ui.dialogs.ExceptionDialog;
import com.softmodeler.ui.dialogs.ValidateDialog;
import com.softmodeler.ui.editors.EObjectEditorInput;
import com.softmodeler.ui.editors.IObjectEditor;
import com.softmodeler.ui.editors.IObjectRefEditor;
import com.softmodeler.ui.editors.UIViewerEditor;
import com.softmodeler.ui.editors.handler.EditorPartHandler;
import com.softmodeler.ui.editors.handler.EditorPartHandler.EditorDefinition;
import com.softmodeler.ui.help.WorkbenchHelpSystem;
import com.softmodeler.ui.internal.UIMessages;
import com.softmodeler.ui.perspective.EditPerspective;

/**
 * Utility for user interface specific tasks
 * 
 * @author created by Author: fdo, last update by $Author: fdo $
 * @version $Revision: 18533 $, $Date: 2013-03-14 16:36:13 +0100 (Thu, 14 Mar 2013) $
 */
public class UIUtil {
	/** Extension Point ID for uiDefinition */
	public static final String EXT_UI_DEFINITION = "com.softmodeler.ui.uiDefinition"; //$NON-NLS-1$
	/** Extension Point id attribute */
	public static final String EXT_ID = "id"; //$NON-NLS-1$
	/** Extension Point filer attribute */
	public static final String EXT_FILTER = "filter"; //$NON-NLS-1$
	/** Extension Point file attribute */
	public static final String EXT_FILE = "file"; //$NON-NLS-1$
	/** Extension Point type 'editor' */
	public static final String EXT_TYPE_EDITOR = "editor"; //$NON-NLS-1$
	/** Extension Point type 'include' */
	public static final String EXT_TYPE_INCLUDE = "include"; //$NON-NLS-1$

	/** property key if object should not unlock after editor is closed */
	public static final String NO_UNLOCK = "noUnlock"; //$NON-NLS-1$

	/** ui definition file extension */
	public static final String UIDEFINITION_EXT = ".xmi"; //$NON-NLS-1$

	/** Identifier for model labels */
	private static final String LABEL_ID = ".label"; //$NON-NLS-1$

	/** the take over permission key */
	private static final String TAKEOVER_PERMISSION = "TAKEOVER"; //$NON-NLS-1$

	/** object image base directory */
	private static final String OBJECT_IMAGE_BASE = "obj16"; //$NON-NLS-1$
	/** object image nature base directory */
	private static final String NATURE_IMAGE_BASE = "var16"; //$NON-NLS-1$
	/** object image file extension */
	private static final String OBJECT_IMAGE_EXT = ".png"; //$NON-NLS-1$

	/** the cached accessControlService */
	private static IAccessControlService accessControlService = null;
	/** help framework instance */
	private static WorkbenchHelpSystem helpFramework;

	/**
	 * returns the access control service
	 * 
	 * @return
	 */
	private static IAccessControlService getAccessControlService() {
		if (accessControlService == null) {
			accessControlService = CommonPlugin.getService(IAccessControlService.class);
		}
		return accessControlService;
	}

	/**
	 * returns the help system, initialize if not done
	 * 
	 * @return
	 */
	public static IWorkbenchHelpSystem getHelpSystem() {
		if (helpFramework == null) {
			helpFramework = new WorkbenchHelpSystem();
		}
		return helpFramework;
	}

	/**
	 * Opens an editor for a BasicObject
	 * 
	 * @param object
	 * @throws WorkbenchException
	 * @throws ServerException
	 */
	public static IEditorPart openEditor(EObject object) throws Exception {
		return openEditorWithHint(object, null);
	}

	/**
	 * Opens an editor for an EObject
	 * 
	 * @param object
	 * @param hint a string that is passed over to the EditorPartHandlers to determine the suitable handlers for opening
	 * this object
	 * @throws WorkbenchException
	 * @throws ServerException
	 */
	public static IEditorPart openEditorWithHint(EObject object, String hint) throws Exception {
		IAccessDecision accessDecision = getAccessControlService().checkAccessById(IdUtil.getUuid(object));

		EditorDefinition[] editorDefinitions = EditorPartHandler.determineEditorDefinitions(object, accessDecision, hint);

		String message = accessDecision.getMessage();
		if (editorDefinitions.length == 0) {
			MessageDialog.openInformation(null, UIMessages.get().UIUtil_AccessDenied, message != null ? message
					: UIMessages.get().UIUtil_AccessDeniedDescription);
			return null;
		}

		// if a special handler is desired and it is available it will be executed
		// as long as the object is not modified by someone else (when TAKEOVER permission is present) 
		EditorDefinition selectedAction = null;
		if (hint != null) {
			for (EditorDefinition definition : editorDefinitions) {
				if (definition.getEditorId().equals(hint)) {
					selectedAction = definition;
					break;
				}
			}
		}

		if (selectedAction == null) {
			selectedAction = editorDefinitions[0];

			if (editorDefinitions.length > 1 || message != null) {
				// if more than one possible action, let the user choose:
				ListDialog actionDialog = new ListDialog(null);
				actionDialog.setTitle(UIMessages.get().UIUtil_AvailableActions);
				actionDialog.setMessage(message);
				actionDialog.setContentProvider(new ArrayContentProvider());
				actionDialog.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						return ((EditorDefinition) element).getActionName();
					}
				});
				actionDialog.setInput(editorDefinitions);
				actionDialog.setInitialSelections(new EditorDefinition[] { selectedAction });

				if (actionDialog.open() == Window.OK) {
					Object[] result = actionDialog.getResult();
					if (result.length == 1) {
						selectedAction = (EditorDefinition) result[0];
					}
				} else {
					return null; // nothing will be opened
				}
			}
		}

		if (selectedAction == null) {
			throw new IllegalStateException("selected action must not be null"); //$NON-NLS-1$
		}

		if (TAKEOVER_PERMISSION.equals(selectedAction.getId())) {
			takeOverWork(object);
		}

		return openEditor(selectedAction.getEditorInput(), selectedAction.getEditorId(), selectedAction.getPerspectiveId());
	}

	/**
	 * Opens an editor assigning listeners to update the viewer on events (rename, delete)
	 * 
	 * @param objectRef
	 * @param viewer
	 * @param treeNode
	 * @throws WorkbenchException
	 * @throws ServerException
	 */
	public static void openEditor(final ObjectRef objectRef, final StructuredViewer viewer, final TreeNode treeNode) throws Exception {
		if (objectRef != null) {
			IEditorPart editorPart = openEditor(objectRef);

			if (editorPart instanceof IObjectEditor) {
				((IObjectEditor) editorPart).setProperty(IObjectEditor.SOURCE_VIEWER, viewer);
				((IObjectEditor) editorPart).setProperty(IObjectEditor.SOURCE_NODE, treeNode);
			}

			if (editorPart instanceof IWorkbenchPart3) {
				editorPart.addPropertyListener(new IPropertyListener() {
					@Override
					public void propertyChanged(Object source, int propId) {
						// update viewers
						if (IEditorPart.PROP_DIRTY == propId && source instanceof IObjectRefEditor) {
							IObjectRefEditor editor = (IObjectRefEditor) source;
							ObjectRef editorObjectRef = editor.getObjectRef();
							if (editor.isDirty() || editorObjectRef == null) {
								return;
							}
							if (treeNode != null) {
								if (treeNode.getObject() != editorObjectRef) {
									// replace TreeNode.objectRef
									treeNode.setObject(editorObjectRef);
									viewer.refresh(treeNode, true);
								}
							} else {
								// update changeable values
								objectRef.setLabels(editorObjectRef.getLabels());
								objectRef.setState(editorObjectRef.getState());
								objectRef.setNature(editorObjectRef.getNature());
								viewer.refresh(objectRef);
							}
						}
					}
				});
			}
		}
	}

	/**
	 * Open the Viewer Editor
	 * 
	 * @param objectRef
	 * @return
	 * @throws PartInitException
	 */
	public static IEditorPart openViewerEditor(ObjectRef objectRef) throws WorkbenchException {
		IEditorInput input = new EObjectEditorInput(objectRef);
		return openEditor(input, UIViewerEditor.ID, EditPerspective.ID);
	}

	/**
	 * opens an actual editor (activate if already open)
	 * 
	 * @param input
	 * @param editorId the editor type id
	 * @param perspectiveId perspective which should be shown, may be null
	 * @return
	 * @throws WorkbenchException
	 */
	public static IEditorPart openEditor(IEditorInput input, String editorId, String perspectiveId) throws WorkbenchException {
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();

		// show associated perspective
		if (perspectiveId != null) {
			IPerspectiveDescriptor perspective = workbench.getPerspectiveRegistry().findPerspectiveWithId(perspectiveId);
			if (perspective != null) {
				page.setPerspective(perspective);
			}
		}

		// if editor is already opened, activate
		for (IEditorReference element : page.getEditorReferences()) {
			if (element.getEditorInput().equals(input)) {
				page.activate(element.getEditor(true));
				return element.getEditor(false);
			}
		}
		return page.openEditor(input, editorId);
	}

	/**
	 * open the URL in an internal window, use the object to create the shell title
	 * 
	 * @param objectRef object used to create title
	 * @param language title language
	 * @param width width in pixels or percentage (0.8 means 80% of client area's width)
	 * @param height height in pixels or percentage (0.8 means 80% of client area's height)
	 * @param openObjectFunctionEnabled activates object links
	 * @param url the URL that will be displayed
	 * @return
	 */
	public static BrowserWindow openOutputPreviewWindow(ObjectRef objectRef, String language, double width, double height,
			boolean objectLinksEnabled, String url) {
		return openObjectOutputWindow(UIMessages.get().UIUtil_PreviewTitle, objectRef, language, width, height, objectLinksEnabled, url);
	}

	/**
	 * open the URL in an internal window, use the object to create the shell title
	 * 
	 * @param titlePrefix
	 * @param objectRef object used to create title
	 * @param language title language
	 * @param width width in pixels or percentage (0.8 means 80% of client area's width)
	 * @param height height in pixels or percentage (0.8 means 80% of client area's height)
	 * @param openObjectFunctionEnabled activates object links
	 * @param url the URL that will be displayed
	 * @return
	 */
	public static BrowserWindow openObjectOutputWindow(String titlePrefix, ObjectRef objectRef, String language, double width,
			double height, boolean objectLinksEnabled, String url) {
		StringBuilder sb = new StringBuilder();
		if (titlePrefix != null && !titlePrefix.isEmpty()) {
			sb.append(titlePrefix).append(' ');
		}

		String classifierLabel = UIUtil.getObjectLabel(objectRef);
		if (classifierLabel != null) {
			sb.append(' ').append(classifierLabel);
		}
		if (sb.length() > 0) {
			sb.append(": "); //$NON-NLS-1$
		}
		sb.append(objectRef.getLabel(language, true));
		sb.append(" (").append(objectRef.getId()).append(" / ").append(LocaleUtil.getLanguageCodeFromString(language).getName(language)) //$NON-NLS-1$ //$NON-NLS-2$
				.append(')');

		return openInternalBrowserWindow(sb.toString(), width, height, objectLinksEnabled, url);
	}

	/**
	 * open the URL in an internal window
	 * 
	 * @param title shell title
	 * @param width width in pixels or percentage (0.8 means 80% of client area's width)
	 * @param height height in pixels or percentage (0.8 means 80% of client area's height)
	 * @param openObjectFunctionEnabled activates object links
	 * @param url the URL that will be displayed
	 * @return
	 */
	public static BrowserWindow openInternalBrowserWindow(String title, double width, double height, boolean objectLinksEnabled, String url) {
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		final BrowserWindow browserWindow = new BrowserWindow(activeWorkbenchWindow.getShell(), title, width, height, true, url);
		browserWindow.open();

		final IEditorPart editorPart = activeWorkbenchWindow.getActivePage().getActiveEditor();
		activeWorkbenchWindow.getActivePage().addPartListener(new IPartListener() {

			@Override
			public void partOpened(IWorkbenchPart part) {
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
			}

			@Override
			public void partClosed(IWorkbenchPart part) {
				if (part == editorPart) {
					browserWindow.close();
				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
			}

			@Override
			public void partActivated(IWorkbenchPart part) {
			}
		});

		return browserWindow;
	}

	/**
	 * refresh the source viewer of the opened objectRef
	 * 
	 * @param editor
	 * @param objectRef
	 */
	public static void refreshViewer(IObjectRefEditor editor, ObjectRef objectRef) {
		StructuredViewer sourceViewer = (StructuredViewer) editor.getProperty(IObjectEditor.SOURCE_VIEWER);
		Object sourceNode = editor.getProperty(IObjectEditor.SOURCE_NODE);

		UIUtil.refreshViewer(objectRef, sourceViewer, sourceNode instanceof TreeNode ? (TreeNode) sourceNode : null);
	}

	/**
	 * refresh the source viewer of an ObjectRef
	 * 
	 * @param objectRef
	 * @param viewer
	 * @param treeNode
	 */
	public static void refreshViewer(ObjectRef objectRef, StructuredViewer viewer, TreeNode treeNode) {
		if (viewer instanceof AbstractTreeViewer) {
			EClassifier classifier = ModelUtil.getClassifier(objectRef);
			AbstractTreeViewer treeViewer = (AbstractTreeViewer) viewer;
			for (Object element : treeViewer.getVisibleExpandedElements()) {
				if (element instanceof TreeNode) {
					refreshTreeNode(treeViewer, (TreeNode) element, treeNode);
				} else if (element instanceof EClassifier && classifier.equals(element)) {
					viewer.refresh(element);
					break;
				}
			}
		} else if (viewer != null) {
			viewer.refresh();
		}
	}

	/**
	 * refresh the source tree
	 * 
	 * @param viewer
	 * @param expandedNode
	 * @param treeNode
	 */
	private static void refreshTreeNode(AbstractTreeViewer viewer, TreeNode expandedNode, TreeNode treeNode) {
		TreeChangeTracker.updateManipulationTime(null);

		for (TreeNodeChild child : expandedNode.getChilds()) {
			if (child.getNodeId().equals(treeNode.getId())) {
				viewer.refresh(child.getParent());
				viewer.expandToLevel(child.getParent(), 1);
				break;
			}
		}
	}

	/**
	 * current user takes over the work
	 * 
	 * @param object
	 * @throws ServerException
	 */
	protected static void takeOverWork(EObject object) throws Exception {
		String objectId = null;
		if (object instanceof ObjectRef) {
			ObjectRef objectRef = (ObjectRef) object;
			objectId = objectRef.getId();
			ModificationInfoUtil.addModificationInfo(objectRef);
		} else if (object instanceof BasicObject) {
			objectId = ((BasicObject) object).getId();
		} else {
			throw new IllegalArgumentException("Objekt is neither an ObjectRef or a BasicObject"); //$NON-NLS-1$
		}

		IModificationInfoService modificationInfoService = CommonPlugin.getService(IModificationInfoService.class);
		modificationInfoService.takeOverWork(objectId);
	}

	/**
	 * Loads an UIDefinition according to the passed object
	 * 
	 * @param object
	 * @param definitionType
	 * @return
	 */
	public static UIDefinition loadUIDefinition(EObject object, String definitionType) {
		EClassifier classifier = object instanceof ObjectRef ? ModelUtil.getClassifier((ObjectRef) object) : object.eClass();
		return loadUIDefinition(classifier, definitionType);
	}

	/**
	 * Loads an UIDefinition according to the passed classifier
	 * 
	 * @param classifier
	 * @param definitionType (editor, dialog, include, page)
	 * @return
	 */
	public static UIDefinition loadUIDefinition(EClassifier classifier, String definitionType) {
		IConfigurationElement nullFilterElement = null;
		IConfigurationElement[] configuration = ExtensionPointUtil.getElements(EXT_UI_DEFINITION, definitionType);
		for (IConfigurationElement type : configuration) {
			for (IConfigurationElement element : ExtensionPointUtil.getChildren(type)) {
				String filter = element.getAttribute(EXT_FILTER);
				if (ModelUtil.isFilterEnabled(filter, classifier)) {
					if (filter == null) {
						nullFilterElement = element;
					} else {
						return loadDefinition(element);
					}
				}
			}
		}
		if (nullFilterElement != null) {
			return loadDefinition(nullFilterElement);
		}
		throw new UIException("Could not find a registered UIDefinition for {0}", classifier.getName()); //$NON-NLS-1$
	}

	/**
	 * Loads an UIDefinition according to the passed path
	 * 
	 * @param definitionId
	 * @return
	 */
	public static UIDefinition loadUIDefinition(String definitionId) {
		IConfigurationElement[] configuration = ExtensionPointUtil.getElements(EXT_UI_DEFINITION);
		for (IConfigurationElement type : configuration) {
			for (IConfigurationElement element : ExtensionPointUtil.getChildren(type)) {
				if (element.getAttribute(EXT_ID).equals(definitionId)) {
					return loadDefinition(element);
				}
			}
		}
		throw new UIException("Could not find a registered UIDefinition for {0}", definitionId); //$NON-NLS-1$
	}

	/**
	 * Load the effective UIDefinition
	 * 
	 * @param element
	 * @return
	 */
	protected static UIDefinition loadDefinition(IConfigurationElement element) {
		try {
			String file = element.getAttribute(EXT_FILE);
			String bundleName = element.getContributor().getName();
			InputStream stream = ResourceUtil.getStream(bundleName, file);
			if (stream == null) {
				throw new UIException("UIDefinition not found {0}/{1}", bundleName, file); //$NON-NLS-1$
			}
			UIDefinition uiDefinition = UIFactory.eINSTANCE.createUIDefinition();
			uiDefinition.init(stream);
			stream.close();

			String root = file.substring(0, file.lastIndexOf(UIDEFINITION_EXT));
			Properties messages = MessagesLoader.load(bundleName, root);
			uiDefinition.setProperties(messages);

			return uiDefinition;
		} catch (Exception e) {
			throw new UIException("error loading ui definition", e); //$NON-NLS-1$
		}
	}

	/**
	 * returns the image for the passed classifierName
	 * 
	 * @param classifierName the classifier name or expression classifier name + nature separated by a space
	 * @return
	 */
	public static Image getObjectImage(String classifierName) {
		ImageDescriptor imageDescriptor = getObjectImageDescriptor(classifierName);
		if (imageDescriptor != null) {
			return UIResourceManager.getInstance().getImage(imageDescriptor);
		}
		return null;
	}

	/**
	 * returns the image for the passed classifier
	 * 
	 * @param classifier
	 * @return
	 */
	public static Image getObjectImage(EClassifier classifier) {
		ImageDescriptor imageDescriptor = getObjectImageDescriptor(classifier);
		if (imageDescriptor != null) {
			return UIResourceManager.getInstance().getImage(imageDescriptor);
		}
		return null;
	}

	/**
	 * Returns the image of a specific {@link ObjectRef}
	 * 
	 * @param objectRef
	 * @return
	 */
	public static Image getObjectImage(ObjectRef objectRef) {
		ImageDescriptor imageDescriptor = getObjectImageDescriptor(ModelUtil.getClassifier(objectRef), objectRef.getNature());
		if (imageDescriptor != null) {
			return UIResourceManager.getInstance().getImage(imageDescriptor);
		}
		return null;
	}

	/**
	 * Returns a ImageDescriptor for an class, searches the customer plug-in first if no image is found the default
	 * image is read
	 * 
	 * @param classifierName the classifier name or expression classifier name + nature separated by a space
	 * @return
	 */
	public static ImageDescriptor getObjectImageDescriptor(String classifierName) {
		int spacepos = classifierName.indexOf(' ');
		if (spacepos > 0) {
			return getObjectImageDescriptor(classifierName.substring(0, spacepos), classifierName.substring(spacepos + 1));
		}
		return getObjectImageDescriptor(ModelUtil.getClassifier(classifierName));
	}

	/**
	 * Returns a ImageDescriptor for an classifier
	 * 
	 * @param classifier
	 * @return
	 */
	public static ImageDescriptor getObjectImageDescriptor(EClassifier classifier) {
		return getObjectImageDescriptor(classifier, null);
	}

	/**
	 * Returns the ImageDescriptor of a specific {@link ObjectRef}
	 * 
	 * @param objectRef
	 * @return
	 */
	public static ImageDescriptor getObjectImageDescriptor(ObjectRef objectRef) {
		return getObjectImageDescriptor(ModelUtil.getClassifier(objectRef), objectRef.getNature());
	}

	/**
	 * Returns a ImageDescriptor for an classifier, adds decoration according to the nature
	 * 
	 * @param classifier
	 * @param nature may be null
	 * @return
	 */
	public static ImageDescriptor getObjectImageDescriptor(String classifierName, String nature) {
		EClass classifierClass = ModelUtil.getClassifier(classifierName);
		return getObjectImageDescriptor(classifierClass, nature);
	}

	/**
	 * Returns a ImageDescriptor for an classifier, adds decoration according to the nature
	 * 
	 * @param classifier
	 * @param nature may be null
	 * @return
	 */
	public static ImageDescriptor getObjectImageDescriptor(EClassifier classifier, String nature) {
		if (ModelUtil.isObjectRef(classifier)) {
			throw new IllegalArgumentException("can not use the ObjectRef classifier to lookup icon, use getObjectImage(ObjectRef)."); //$NON-NLS-1$
		}

		String classifierName = ModelUtil.getClassifierName(classifier);
		String bundleName = ModelUtil.getBundleName(classifier.getEPackage());
		if (bundleName == null) {
			if (ExtensionModelUtil.isExtendedClassifier(classifier)) {
				EClass originalClassifier = ExtensionModelUtil.getOriginalClassifier(classifier);
				if (originalClassifier != null) {
					bundleName = ModelUtil.getBundleName(originalClassifier.getEPackage());
				}
			}
			if (bundleName == null) {
				return null;
			}
		}
		UIResourceManager resourceManager = UIResourceManager.getInstance();

		String path = UIPlugin.ICONS_PATH + OBJECT_IMAGE_BASE + PathUtil.SEP + classifierName + OBJECT_IMAGE_EXT;
		ImageDescriptor imageDescriptor = resourceManager.getPluginImageDescriptor(bundleName, path);
		if (imageDescriptor != null && nature != null) {
			String naturePath = UIPlugin.ICONS_PATH + NATURE_IMAGE_BASE + PathUtil.SEP + nature + OBJECT_IMAGE_EXT;
			ImageDescriptor natureDescriptor = resourceManager.getPluginImageDescriptor(bundleName, naturePath);
			if (natureDescriptor != null) {
				// decoration overlay icon returns the same icon in case it would be the same end icon, only when using UIResourceManager
				imageDescriptor = new DecorationOverlayIcon(resourceManager.getImage(imageDescriptor), natureDescriptor,
						IDecoration.TOP_LEFT);
			}
		}
		return imageDescriptor;
	}

	/**
	 * Returns the label of a specific class
	 * 
	 * @param classifier
	 * @return
	 */
	public static String getObjectLabel(EClassifier classifier) {
		if (ModelUtil.isObjectRef(classifier)) {
			throw new IllegalArgumentException("can not use the ObjectRef classifier to lookup label, use getObjectLabel(ObjectRef)."); //$NON-NLS-1$
		}
		return getObjectLabel(classifier.getName());
	}

	/**
	 * Returns the label of a specific {@link ObjectRef}
	 * 
	 * @param objectRef
	 * @return
	 */
	public static String getObjectLabel(ObjectRef objectRef) {
		return getObjectLabel(objectRef.getType());
	}

	/**
	 * Returns the label of a specific classifier
	 * 
	 * @param classifierName
	 * @return
	 */
	public static String getObjectLabel(String classifierName) {
		String label = LabelStorage.get(classifierName + LABEL_ID);
		return label != null ? label : classifierName;
	}

	/**
	 * Is the object type enabled
	 * 
	 * @param classifier
	 * @return
	 */
	public static boolean isObjectEnabled(EClass classifier) {
		return !TypeInfos.getInstance().hasFlag(classifier, TypeInfos.INVISIBLE);
	}

	/**
	 * Is the object type enabled
	 * 
	 * @param classifierName
	 * @return
	 */
	public static boolean isObjectEnabled(String classifierName) {
		return isObjectEnabled(ModelUtil.getClassifier(classifierName));
	}

	/**
	 * Handles exceptions<br />
	 * Prints the Stack-Trace, logs the exception and opens an ErrorDialog
	 * 
	 * @param e the thrown exception
	 * @param shell the parent shell
	 */
	public static void handleException(final Throwable e, Shell shell) {
		if (e instanceof org.eclipse.riena.communication.core.RemoteFailure) {
			MessageDialog.openError(shell, UIMessages.get().UIUtil_CommunicationError, UIMessages.get().UIUtil_CommunicationErrorMessage);
			return;
		}
		if (e instanceof UndeclaredThrowableException) {
			handleException(((UndeclaredThrowableException) e).getUndeclaredThrowable(), shell);
			return;
		} else if (e instanceof InvocationTargetException) {
			handleException(((InvocationTargetException) e).getTargetException(), shell);
			return;
		} else if (e instanceof ValidationException) {
			ValidateDialog dialog = new ValidateDialog(shell, (ValidationException) e);
			dialog.open();
			return;
		}

		IStatus status = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, e.getMessage(), e);
		UIPlugin.getDefault().getLog().log(status);

		// open exception dialog
		ExceptionDialog dlg = new ExceptionDialog(shell, e, status);
		dlg.open();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see com.softmodeler.ui.UIPlugin#handleException(java.lang.Throwable, org.eclipse.swt.widgets.Shell)
	 */
	public static void handleException(Throwable e, IShellProvider shellProvider) {
		handleException(e, shellProvider.getShell());
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see com.softmodeler.ui.UIPlugin#handleException(java.lang.Throwable, org.eclipse.swt.widgets.Shell)
	 */
	public static void handleException(Throwable e) {
		if (Display.getCurrent() != null) {
			handleException(e, Display.getCurrent().getActiveShell());
		} else {
			logException(e);
		}
	}

	/**
	 * Logs an exception without displaying it to the client
	 * 
	 * @param e
	 */
	public static void logException(Throwable e) {
		if (e instanceof UndeclaredThrowableException) {
			handleException(((UndeclaredThrowableException) e).getUndeclaredThrowable());
			return;
		} else if (e instanceof InvocationTargetException) {
			handleException(((InvocationTargetException) e).getTargetException());
			return;
		}

		IStatus status = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, e.getMessage(), e);
		if (UIPlugin.getDefault() != null) {
			UIPlugin.getDefault().getLog().log(status);
		}
	}

	/**
	 * Returns the filter for individual classes
	 */
	public static String getIndividualFilterString() {
		StringBuffer sb = new StringBuffer();
		List<EClassifier> classifiers = ModelUtil.getIndividualClassifiers();
		for (EClassifier eClassifier : classifiers) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(eClassifier.getName());
		}
		return sb.toString();
	}

	/**
	 * Returns the filter for group classes
	 */
	public static String getGroupFilterString() {
		StringBuffer sb = new StringBuffer();
		List<EClassifier> classifiers = ModelUtil.getGroupClassifiers();
		for (EClassifier eClassifier : classifiers) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(eClassifier.getName());
		}
		return sb.toString();
	}

	/**
	 * sets the date value in a DateTime control
	 * 
	 * @param dateControl
	 * @param date
	 */
	public static void setDate(DateTime dateControl, Date date) {
		Calendar calendar = new GregorianCalendar();
		if (date == null) {
			date = new Date();
		}
		calendar.setTime(date);
		dateControl.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * gets the date value of a DateTime control
	 * 
	 * @param dateControl
	 * @return
	 */
	public static Date getDate(DateTime dateControl) {
		Calendar calendar = new GregorianCalendar(dateControl.getYear(), dateControl.getMonth(), dateControl.getDay());
		return calendar.getTime();
	}

	/**
	 * enable/disable an activity
	 * 
	 * @param activityId activity id
	 * @param enabled
	 */
	public static void setActivityEnabled(String activityId, boolean enabled) {
		if (enabled) {
			enableActivity(activityId);
		} else {
			disableActivity(activityId);
		}

	}

	/**
	 * disable an activity by id
	 * 
	 * @param activityId
	 */
	@SuppressWarnings("unchecked")
	public static void disableActivity(String activityId) {
		IWorkbenchActivitySupport activitySupport = PlatformUI.getWorkbench().getActivitySupport();
		Set<String> enabledActivityIds = activitySupport.getActivityManager().getEnabledActivityIds();

		Set<String> ids = new HashSet<String>(enabledActivityIds);
		for (Iterator<String> iterator = ids.iterator(); iterator.hasNext();) {
			String object = iterator.next();
			if (object.equals(activityId)) {
				iterator.remove();
				break;
			}
		}
		activitySupport.setEnabledActivityIds(ids);
	}

	/**
	 * enable an activity by id
	 * 
	 * @param activityId
	 */
	@SuppressWarnings("unchecked")
	public static void enableActivity(String activityId) {
		IWorkbenchActivitySupport activitySupport = PlatformUI.getWorkbench().getActivitySupport();
		Set<String> enabledActivityIds = activitySupport.getActivityManager().getEnabledActivityIds();
		if (!enabledActivityIds.contains(activityId)) {
			Set<String> ids = new HashSet<String>(enabledActivityIds);
			ids.add(activityId);
			activitySupport.setEnabledActivityIds(ids);
		}
	}
}
