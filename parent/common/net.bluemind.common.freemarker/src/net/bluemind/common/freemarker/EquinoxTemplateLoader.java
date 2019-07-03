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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.common.freemarker;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.slf4j.LoggerFactory;

import freemarker.cache.URLTemplateLoader;

public class EquinoxTemplateLoader extends URLTemplateLoader {

	private static org.slf4j.Logger logger = LoggerFactory.getLogger(EquinoxTemplateLoader.class);
	private final ClassLoader classLoader;
	private final String path;

	public EquinoxTemplateLoader(ClassLoader classLoader, String basePath) {
		this.classLoader = classLoader;
		this.path = basePath;
	}

	@Override
	protected URL getURL(String name) {
		try {
			Enumeration<URL> values = classLoader.getResources(path + name);
			URL ret = null;
			while (values.hasMoreElements()) {
				ret = values.nextElement();
			}
			return ret;
		} catch (IOException e) {
			logger.error("search resource {}", name, e);
			return classLoader.getResource(name);
		}

	}

}
