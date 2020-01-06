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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible of loading an ini file.
 * 
 * 
 */
public abstract class IniFile {
	private static final String DEFAULT_COMMENT = "";
	private final String path;
	private Properties properties;
	private static final Logger logger = LoggerFactory.getLogger(IniFile.class);

	public IniFile(String path) {
		this.path = path;
	}

	public String getProperty(String key) {
		return getProperties().getProperty(key);
	}

	public void setProperty(String key, String value) {
		getProperties().setProperty(key, value);
	}

	protected abstract String getCategory();

	protected String getComment() {
		return DEFAULT_COMMENT;
	};

	private static Properties load(String path) {
		Properties properties = new Properties();
		try (InputStream is = new FileInputStream(new File(path))) {
			properties.load(is);
		} catch (IOException e) {
			logger.error("Unable to load '{}'", path, e);
		}
		return properties;
	}

	protected void save() {
		try (OutputStream out = new FileOutputStream(new File(this.path))) {
			this.getProperties().store(out, getComment());
		} catch (IOException e) {
			logger.error("Unable to save '{}'", this.path, e);
		}
	}

	public Properties getProperties() {
		if (properties == null) {
			properties = load(this.path);
		}
		return properties;
	}

}
