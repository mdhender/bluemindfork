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
package net.bluemind.core.utils;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.google.common.io.ByteStreams;

public class ImageHandlingTests {

	static {
		System.setProperty("java.awt.headless", "true");
	}

	/**
	 * whatever.png is a 5000x5000 compressed png with just a green rectangle
	 * 
	 * @return
	 * @throws IOException
	 */
	private byte[] whatever() throws IOException {

		try (InputStream in = getClass().getClassLoader().getResourceAsStream("/data/whatever.png")) {
			return ByteStreams.toByteArray(in);
		}
	}

	private byte[] bigForBM16825() throws IOException {

		try (InputStream in = getClass().getClassLoader().getResourceAsStream("/data/bm-16825.png")) {
			return ByteStreams.toByteArray(in);
		}
	}

	@Test
	public void testCheckAndSanitizeThenResize() throws IOException {
		assertNotNull(whatever());
		ImageUtils.checkAndSanitize(whatever());
		ImageUtils.resize(whatever(), 500, 50);

	}

	@Test
	public void testCheckAndSanitizeBM16825() throws IOException {
		assertNotNull(bigForBM16825());
		ImageUtils.checkAndSanitize(bigForBM16825());
		ImageUtils.resize(bigForBM16825(), 500, 50);

	}

}
