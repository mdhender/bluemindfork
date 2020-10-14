/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.webmodule.uploadhandler.internal;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class Subsampling {

	private Subsampling() {
	}

	public static BufferedImage toBufferredImage(ImageInputStream iis) throws IOException {
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
			param.setSourceSubsampling(subsampling, subsampling, 0, 0);
		}
		return reader.read(0, param);
	}

}
