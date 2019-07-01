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
package net.bluemind.locator.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NonOsgiActivator {

	private static final Logger logger = LoggerFactory.getLogger(NonOsgiActivator.class);

	private static NonOsgiActivator inst;

	static {
		try {
			inst = new NonOsgiActivator();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			inst = null;
		}
	}

	public static NonOsgiActivator get() {
		return inst;
	}

	private String locatorUrl;

	private String host;

	private NonOsgiActivator() throws IOException {
		if (!new File("/etc/bm/bm.ini").exists()) {
			host = "localhost";
			locatorUrl = "http://localhost:8084/";
			logger.error("bm.ini file not found");
			return;
		}

		Properties p = new Properties();
		FileInputStream fis = new FileInputStream("/etc/bm/bm.ini");
		try {
			p.load(fis);
			String h = p.getProperty("locator");
			if (h == null) {
				h = p.getProperty("host");
			}
			if (h == null) {
				throw new IOException("No host or locator property found in /etc/bm/bm.ini");
			}
			locatorUrl = "http://" + h + ":8084/";
			host = h;
		} finally {
			fis.close();
		}
	}

	public String getLocatorUrl() {
		return locatorUrl;
	}

	public String getHost() {
		return host;
	}
}
