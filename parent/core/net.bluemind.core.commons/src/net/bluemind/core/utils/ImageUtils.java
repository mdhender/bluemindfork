/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class ImageUtils {

	/**
	 * Checks the byte array is in a supported image format and returns a PNG
	 * version
	 * 
	 * @param icon
	 * @return
	 * @throws ServerFault
	 */
	public static byte[] checkAndSanitize(byte[] icon) throws ServerFault {
		try {
			BufferedImage bi = ImageIO.read(new ByteArrayInputStream(icon));
			if (bi == null) {
				throw new ServerFault("not an image (not valid format)", ErrorCode.INVALID_PARAMETER);
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream(icon.length);
			ImageIO.write(bi, "png", baos);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new ServerFault("not an image (" + e.getMessage() + ")", ErrorCode.INVALID_PARAMETER);
		}

	}

	public static byte[] resize(byte[] image, int width, int height) throws ServerFault {

		try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
			Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
			if (!iter.hasNext()) {
				throw new ServerFault("not an image", ErrorCode.INVALID_PARAMETER);
			}
			ImageReader reader = (ImageReader) iter.next();

			reader.setInput(iis);
			BufferedImage img = reader.read(0);
			while (img.getWidth() != width && img.getHeight() != height) {
				double xscale = (double) width / (double) img.getWidth();
				double yscale = (double) height / (double) img.getHeight();
				if (xscale < 0.5) {
					xscale = 0.5;
				}

				if (yscale < 0.5) {
					yscale = 0.5;
				}

				BufferedImage dbi = new BufferedImage((int) ((double) img.getWidth() * xscale),
						(int) ((double) img.getHeight() * yscale), img.getType());
				Graphics2D g = dbi.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				AffineTransform at = AffineTransform.getScaleInstance(xscale, yscale);

				g.drawRenderedImage(img, at);
				img = dbi;
			}

			ByteArrayOutputStream ret = new ByteArrayOutputStream();
			ImageIO.write(img, "png", ret);
			return ret.toByteArray();
		} catch (IOException e) {
			throw new ServerFault("not an image (" + e.getMessage() + ")", ErrorCode.INVALID_PARAMETER);
		}
	}
}
