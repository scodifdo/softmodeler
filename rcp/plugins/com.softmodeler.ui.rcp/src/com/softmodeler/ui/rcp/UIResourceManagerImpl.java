/*******************************************************************************
 * $URL: $
 * 
 * Copyright (c) 2007 henzler informatik gmbh, CH-4106 Therwil
 *******************************************************************************/
package com.softmodeler.ui.rcp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.softmodeler.common.util.PathUtil;
import com.softmodeler.common.util.ResourceUtil;
import com.softmodeler.ui.UIResourceManager;

/**
 * @author created by Author: fdo, last update by $Author: $
 * @version $Revision: $, $Date: $
 */
public class UIResourceManagerImpl extends UIResourceManager {
	private static final int MISSING_IMAGE_SIZE = 10;

	private Map<String, ImageDescriptor> imageDescriptors = new HashMap<String, ImageDescriptor>();

	private Map<String, Image> imageMap = new HashMap<String, Image>();

	@Override
	public Color getColor(int systemColorID) {
		Display display = Display.getCurrent();
		return display.getSystemColor(systemColorID);
	}

	@Override
	public Color getColor(int r, int g, int b) {
		return getColor(new RGB(r, g, b));
	}

	@Override
	public Color getColor(RGB rgb) {
		return getResourceManager().createColor(rgb);
	}

	/**
	 * creates an image with the passed stream
	 * 
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	protected Image getImage(InputStream stream) throws IOException {
		try {
			Display display = Display.getCurrent();
			ImageData data = new ImageData(stream);
			if (data.transparentPixel > 0) {
				return new Image(display, data, data.getTransparencyMask());
			}
			return new Image(display, data);
		} finally {
			stream.close();
		}
	}

	@Override
	public Image getImage(String path) {
		Image image = imageMap.get(path);
		if (image == null) {
			try {
				image = getImage(new FileInputStream(path));
				imageMap.put(path, image);
			} catch (Exception e) {
				image = getMissingImage();
				imageMap.put(path, image);
			}
		}
		return image;
	}

	private Image getMissingImage() {
		Image image = new Image(Display.getCurrent(), MISSING_IMAGE_SIZE, MISSING_IMAGE_SIZE);

		GC gc = new GC(image);
		gc.setBackground(getColor(SWT.COLOR_RED));
		gc.fillRectangle(0, 0, MISSING_IMAGE_SIZE, MISSING_IMAGE_SIZE);
		gc.dispose();

		return image;
	}

	@Override
	public Font getFont(String name, int size, int style) {
		FontDescriptor descriptor = FontDescriptor.createFrom(name, size, style);
		return getResourceManager().createFont(descriptor);
	}

	@Override
	public void dispose() {
		for (Image image : imageMap.values()) {
			image.dispose();
		}
		imageMap.clear();

		imageDescriptors.clear();
	}

	@Override
	public Image getImage(ImageDescriptor descriptor) {
		if (descriptor == null) {
			return null;
		}
		return getResourceManager().createImage(descriptor);
	}

	@Override
	public void removeImage(ImageDescriptor descriptor) {
		getResourceManager().destroyImage(descriptor);
	}

	@Override
	public ImageDescriptor getImageDescriptor(String path, InputStream in) {
		if (imageDescriptors.containsKey(path)) {
			return imageDescriptors.get(path);
		}
		ImageData imageData = new ImageData(in);
		ImageDescriptor imageDescriptor = ImageDescriptor.createFromImageData(imageData);
		imageDescriptors.put(path, imageDescriptor);
		return imageDescriptor;
	}

	@Override
	protected ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptors.get(path);
	}

	@Override
	public boolean removeImageDescriptor(String path) {
		return imageDescriptors.remove(path) != null;
	}

	@Override
	public Image getPluginImage(String symbolicName, String path) {
		return getImage(getPluginImageDescriptor(symbolicName, path));
	}

	@Override
	public ImageDescriptor getPluginImageDescriptor(String symbolicName, String path) {
		String key = symbolicName + PathUtil.SEP + path;
		if (imageDescriptors.containsKey(key)) {
			return imageDescriptors.get(key);
		}
		try {
			InputStream inputStream = ResourceUtil.getBundleResource(symbolicName, path.startsWith("$nl$") ? path.substring(5) : path); //$NON-NLS-1$
			if (inputStream == null) {
				// no image found
				return null;
			}
			ImageDescriptor imageDescriptor = ImageDescriptor.createFromImageData(new ImageData(inputStream));
			imageDescriptors.put(key, imageDescriptor);
			inputStream.close();
			return imageDescriptor;
		} catch (Exception e) {
			throw new IllegalStateException(NLS.bind("error reading resource {0}/{1}", symbolicName, path), e); //$NON-NLS-1$
		}
	}

}
