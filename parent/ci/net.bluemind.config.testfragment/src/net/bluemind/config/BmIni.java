/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BmIni {

	private static final Logger logger = LoggerFactory.getLogger(BmIni.class);
	private static String bmIni = "data/bm.ini";

	public static Map<String, String> get() {
		Properties props = new Properties();
		File iniFile = new File(bmIni);
		if (iniFile.exists()) {
			try (InputStream in = Files.newInputStream(iniFile.toPath())) {
				props.load(in);

			} catch (IOException e) {
				logger.error("error during loading bm.ini", e);
			}
		} else {
			logger.warn("data/bm.ini not found");
		}

		Map<String, String> values = new HashMap<String, String>();

		for (Entry<Object, Object> entry : props.entrySet()) {
			values.put((String) entry.getKey(), (String) entry.getValue());
		}
		return values;
	}

	public static String value(String key) {
		Properties p = new Properties();
		File iniFile = new File(bmIni);
		if (iniFile.exists()) {
			try (InputStream in = Files.newInputStream(iniFile.toPath())) {
				p.load(in);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else {
			logger.warn("data/bm.ini not found");
		}
		return p.getProperty(key);
	}

}
