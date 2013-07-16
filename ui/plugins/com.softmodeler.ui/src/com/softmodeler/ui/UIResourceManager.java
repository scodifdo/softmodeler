package com.softmodeler.ui;

import java.io.InputStream;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.osgi.framework.Bundle;

import com.softmodeler.common.CommonPlugin;
import com.softmodeler.common.util.PathUtil;
import com.softmodeler.common.util.RuntimeUtil;
import com.softmodeler.model.BasicCode;
import com.softmodeler.model.Code;
import com.softmodeler.model.type.Base64Type;
import com.softmodeler.service.IObjectImageService;
import com.softmodeler.ui.internal.UIMessages;

/**
 * Utility class for managing OS resources associated with SWT controls such as colors, fonts, images, etc.
 * <p>
 * !!! IMPORTANT !!! Application code must explicitly invoke the <code>dispose()</code> method to release the operating
 * system resources managed by cached objects when those objects and OS resources are no longer needed (e.g. on
 * application shutdown)
 * <p>
 * This class may be freely distributed as part of any application or plugin.
 * <p>
 * Copyright (c) 2003 - 2007, Instantiations, Inc. <br>
 * All Rights Reserved
 * 
 * @author scheglov_ke
 * @author Dan Rubel
 */
public abstract class UIResourceManager {
	/** singleton instance */
	private static UIResourceManager instance;
	/** */
	private static IObjectImageService objectImageService;

	/** local JFace resource manager */
	protected ResourceManager resourceManager = null;

	/**
	 * singleton constructor
	 */
	protected UIResourceManager() {
	}

	/**
	 * Returns the singleton instance, depending on the runtime (RCP/RAP)
	 * 
	 * @return
	 */
	public static UIResourceManager getInstance() {
		if (instance == null) {
			instance = RuntimeUtil.newInstance(UIResourceManager.class);
		}
		return instance;
	}

	/**
	 * returns the local JFace resource Manager
	 * 
	 * @return
	 */
	protected ResourceManager getResourceManager() {
		if (resourceManager == null) {
			resourceManager = new LocalResourceManager(JFaceResources.getResources());
		}
		return resourceManager;
	}

	/**
	 * Returns the system {@link Color} matching the specific ID.
	 * 
	 * @param systemColorID the ID value for the color
	 * @return the system {@link Color} matching the specific ID
	 */
	public abstract Color getColor(int systemColorID);

	/**
	 * Returns a {@link Color} given its red, green and blue component values.
	 * 
	 * @param r the red component of the color
	 * @param g the green component of the color
	 * @param b the blue component of the color
	 * @return the {@link Color} matching the given red, green and blue component values
	 */
	public abstract Color getColor(int r, int g, int b);

	/**
	 * Returns a {@link Color} given its RGB value.
	 * 
	 * @param rgb the {@link RGB} value of the color
	 * @return the {@link Color} matching the RGB value
	 */
	public abstract Color getColor(RGB rgb);

	/**
	 * Returns an {@link Image} stored in the file at the specified path.
	 * 
	 * @param path the path to the image file
	 * @return the {@link Image} stored in the file at the specified path
	 */
	public abstract Image getImage(String path);

	/**
	 * Returns a {@link Font} based on its name, height and style.
	 * 
	 * @param name the name of the font
	 * @param height the height of the font
	 * @param style the style of the font
	 * @return {@link Font} The font matching the name, height and style
	 */
	public abstract Font getFont(String name, int height, int style);

	/**
	 * Dispose of cached objects and their underlying OS resources. This should only be called when the cached objects
	 * are no longer needed (e.g. on application shutdown).
	 */
	public abstract void dispose();

	/**
	 * Returns an {@link ImageDescriptor} for the passed path, if it does not exists creates one using the
	 * {@link InputStream}
	 * 
	 * @param path
	 * @param in
	 * @return
	 */
	public abstract ImageDescriptor getImageDescriptor(String path, InputStream in);

	/**
	 * Returns an {@link ImageDescriptor} stored in the file at the specified path.
	 * 
	 * @param path the path to the image file.
	 * @return the {@link ImageDescriptor} stored in the file at the specified path.
	 */
	protected abstract ImageDescriptor getImageDescriptor(String path);

	/**
	 * Removes an {@link ImageDescriptor} from the cache accoring to the passed path
	 * 
	 * @param path
	 * @return
	 */
	public abstract boolean removeImageDescriptor(String path);

	/**
	 * Returns an {@link Image} based on the specified {@link ImageDescriptor}.
	 * 
	 * @param descriptor the {@link ImageDescriptor} for the {@link Image}.
	 * @return the {@link Image} based on the specified {@link ImageDescriptor}.
	 */
	public abstract Image getImage(ImageDescriptor descriptor);

	/**
	 * removes an existing {@link Image} that was cached with this image descriptor
	 * 
	 * @param descriptor
	 * @return
	 */
	public abstract void removeImage(ImageDescriptor descriptor);

	/**
	 * Returns an {@link Image} based on a {@link Bundle} and resource entry path.
	 * 
	 * @param symbolicName the symbolic name of the {@link Bundle}.
	 * @param path the path of the resource entry.
	 * @return the {@link Image} stored in the file at the specified path.
	 */
	public abstract Image getPluginImage(String symbolicName, String path);

	/**
	 * Returns an {@link ImageDescriptor} based on a {@link Bundle} and resource entry path.
	 * 
	 * @param symbolicName the symbolic name of the {@link Bundle}.
	 * @param path the path of the resource entry.
	 * @return the {@link ImageDescriptor} based on a {@link Bundle} and resource entry path.
	 */
	public abstract ImageDescriptor getPluginImageDescriptor(String symbolicName, String path);

	/**
	 * returns the object image service
	 * 
	 * @return
	 */
	protected static IObjectImageService getObjectImageService() {
		if (objectImageService == null) {
			objectImageService = CommonPlugin.getService(IObjectImageService.class);
		}
		return objectImageService;
	}

	/**
	 * returns the icon of the code or an icon defined in one of the parent codes
	 * 
	 * @param code
	 * @return
	 */
	protected static Base64Type findIcon(BasicCode code) {
		if (code != null) {
			String codeId = code.getId();
			try {
				Base64Type imageData = getObjectImageService().getImageData(codeId);
				if (imageData != null) {
					return imageData;
				}
				if (code.getParent() != null) {
					return findIcon(code.getParent()); // look for icon in parent codes
				}
			} catch (Exception e) {
				UIPlugin.logger.warn(UIMessages.get().UIResourceManager_ErrorLoadingCodeIcon, codeId);
			}
		}
		return null;
	}

	/**
	 * Finds and returns an {@link ImageDescriptor} for the {@link Code} icon
	 * 
	 * @param code
	 * @return
	 */
	public ImageDescriptor getCodeIcon(BasicCode code) {
		String path = code.eClass().getName() + PathUtil.SEP + code.getId();
		ImageDescriptor imageDescriptor = getImageDescriptor(path);
		if (imageDescriptor != null) {
			return imageDescriptor;
		}

		Base64Type icon = findIcon(code);
		if (icon != null) {
			return getImageDescriptor(path, icon.getBinaryStream());
		}
		return null;
	}

	/**
	 * Remove a {@link Code} icon from the cache
	 * 
	 * @param code
	 */
	public void removeCodeIcon(BasicCode code) {
		String path = code.eClass().getName() + PathUtil.SEP + code.getId();
		ImageDescriptor imageDescriptor = getImageDescriptor(path);
		if (imageDescriptor != null) {
			removeImage(imageDescriptor);
		}
		removeImageDescriptor(path);
	}
}
