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
package net.bluemind.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible of loading an ini file.
 * 
 * 
 */
public abstract class IniFile {

	private Map<String, String> settings;
	private Logger logger;

	public IniFile(String path) {
		logger = LoggerFactory.getLogger(getClass());
		settings = new HashMap<String, String>();
		File f = new File(path);
		if (f.exists()) {
			loadIniFile(f);
		} else {
			logger.warn(path + " does not exist.");
		}
	}

	protected String getSetting(String settingName) {
		return settings.get(settingName);
	}

	public Map<String, String> getData() {
		return settings;
	}

	public abstract String getCategory();

	private void loadIniFile(File f) {
		try (FileInputStream in = new FileInputStream(f)) {
			Properties p = new Properties();
			p.load(in);
			for (Object key : p.keySet()) {
				settings.put((String) key, p.getProperty((String) key));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

}
