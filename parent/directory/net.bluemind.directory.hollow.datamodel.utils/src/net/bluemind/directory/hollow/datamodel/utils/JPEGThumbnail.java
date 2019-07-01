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
package net.bluemind.directory.hollow.datamodel.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JPEGThumbnail {

	private static final Logger logger = LoggerFactory.getLogger(JPEGThumbnail.class);

	public static byte[] scaleImage(byte[] sourceImage, int maxWidth, int maxHeight) throws Exception {

		if (sourceImage == null) {
			logger.warn("received null source image. nothing to resize");
			return null;
		}

		Image image = ImageIO.read(new ByteArrayInputStream(sourceImage));

		if (image == null) {
			logger.warn("un-readable image. nothing to resize");
			return null;
		}

		int thumbWidth = maxWidth;
		int thumbHeight = maxHeight;

		if (image.getWidth(null) < maxWidth && image.getHeight(null) < maxHeight) {
			thumbWidth = image.getWidth(null);
			thumbHeight = image.getHeight(null);
		} else {
			// Make sure the aspect ratio is maintained, so the image is not
			// skewed
			double thumbRatio = (double) thumbWidth / (double) thumbHeight;
			int imageWidth = image.getWidth(null);
			int imageHeight = image.getHeight(null);
			double imageRatio = (double) imageWidth / (double) imageHeight;
			if (thumbRatio < imageRatio) {
				thumbHeight = (int) (thumbWidth / imageRatio);
			} else {
				thumbWidth = (int) (thumbHeight * imageRatio);
			}
		}

		// Draw the scaled image
		BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = thumbImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);

		return writeImage(thumbImage);
	}

	private static byte[] writeImage(BufferedImage thumbImage) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(thumbImage, "jpg", out);
		return out.toByteArray();
	}

}
