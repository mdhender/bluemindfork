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
package net.bluemind.xivo.common.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class XivoIni {

	private static final Logger logger = LoggerFactory.getLogger(XivoIni.class);
	private static final Properties p = new Properties();

	static {
		File f = new File("/etc/bm/xivo.ini");
		if (f.exists()) {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(f);
				p.load(fis);
				fis.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
			}
		} else {
			logger.error("/etc/bm/xivo.ini does not exist.");
		}
	}

	public static String val(String k) {
		return p.getProperty(k);
	}

}
