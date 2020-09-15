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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class ImageUtils {

	private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

	private ImageUtils() {

	}

	/**
	 * Checks the byte array is in a supported image format and returns a PNG
	 * version
	 * 
	 * Also rejects the image if the resulting PNG would be bigger than 1MB
	 * 
	 * @param icon
	 * @return
	 * @throws ServerFault
	 */
	public static byte[] checkAndSanitize(byte[] icon) {
		Path tmpFile = null;
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(icon))) {
			BufferedImage bi = toBufferredImage(in);
			if (bi == null) {
				throw new ServerFault("not an image (not valid format)", ErrorCode.INVALID_PARAMETER);
			}
			tmpFile = Files.createTempFile("image-check-and-sanitize", ".png");
			try (OutputStream out = Files.newOutputStream(tmpFile)) {
				ImageIO.write(bi, "png", out);
				if (tmpFile.toFile().length() < 1024 * 1024) {
					return Files.readAllBytes(tmpFile);
				} else {
					throw new ServerFault("Image is too big, original size is " + icon.length + " byte(s)");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new ServerFault("not an image (" + e.getMessage() + ")", ErrorCode.INVALID_PARAMETER);
		} finally {
			if (tmpFile != null) {
				tmpFile.toFile().delete(); // NOSONAR
			}
		}

	}

	public static byte[] resize(byte[] image, int width, int height) {

		try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
			BufferedImage img = toBufferredImage(iis);
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

	private static BufferedImage toBufferredImage(ImageInputStream iis) throws IOException {
		Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);
		if (!iter.hasNext()) {
			throw new ServerFault("not an image", ErrorCode.INVALID_PARAMETER);
		}

		ImageReader reader = iter.next();
		reader.setInput(iis);
		int subsampling = 1;
		double w = reader.getWidth(0);
		double h = reader.getHeight(0);
		if (w > 8000 || h > 8000) {
			subsampling = 8;
		} else if (w > 4000 || h > 4000) {
			subsampling = 4;
		} else if (w > 2000 || h > 2000) {
			subsampling = 2;
		}

		ImageReadParam param = reader.getDefaultReadParam();
		if (subsampling > 1) {
			logger.warn("Subsampling too big source image (sub factor {})", subsampling);
			param.setSourceSubsampling(subsampling, subsampling, 0, 0);
		}
		return reader.read(0, param);
	}
}
