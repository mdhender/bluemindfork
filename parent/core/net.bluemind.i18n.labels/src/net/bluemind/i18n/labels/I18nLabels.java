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
package net.bluemind.i18n.labels;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class I18nLabels {

	static final Logger logger = LoggerFactory.getLogger(I18nLabels.class);

	private static final String[] langs = { "fr" };
	private static final I18nLabels INSTANCE = new I18nLabels();
	private Map<String, String> defaultResource;
	private Map<String, Map<String, String>> resources = new HashMap<>();

	public static I18nLabels getInstance() {
		return INSTANCE;
	}

	private I18nLabels() {
		try {
			loadLang(null);
			for (String lang : langs) {
				loadLang(lang);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void loadLang(String lang) {
		String propFile = "i18n_labels";
		if (lang != null) {
			propFile = propFile + "_" + lang;
		}
		HashMap<String, String> map = new HashMap<>();
		propFile = propFile + ".properties";
		URL url = getClass().getResource("/" + propFile);
		if (logger.isDebugEnabled()) {
			logger.debug("url " + url + " p " + propFile);
		}
		if (lang != null) {
			map.putAll(defaultResource);
		}
		try (InputStream in = url.openStream()) {
			Properties p = new Properties();
			p.load(new InputStreamReader(in, Charset.forName("utf-8")));
			for (Map.Entry<Object, Object> e : p.entrySet()) {
				map.put((String) e.getKey(), (String) e.getValue());
			}
		} catch (IOException e) {
		}
		if (lang == null) {
			defaultResource = map;
		} else {
			resources.put(lang, map);
		}
	}

	public Map<String, String> all(String lang) {
		Map<String, String> r = resources.get(lang);
		if (r != null) {
			return r;
		} else {
			return defaultResource;
		}

	}

	public String translate(String lang, String label) {
		if (label == null) {
			return null;
		}
		if (!(label.startsWith("$$") && label.endsWith("$$"))) {
			return label;
		}
		String id = label.substring(2, label.length() - 2);
		Map<String, String> r = resources.get(lang);
		String value = null;
		if (r != null) {
			value = r.get(id);
		} else {
			value = defaultResource.get(id);
		}

		if (value == null) {
			return label;
		}
		return value;

	}

	public List<String> getMatchingKeys(String name, String lang) {
		String upperCaseName = name.toUpperCase();
		Set<String> keys = new HashSet<>();
		Map<String, String> translations = resources.get(lang);
		if (translations == null) {
			translations = defaultResource;
		}

		for (String key : translations.keySet()) {
			String value = translations.get(key);
			if (value.toUpperCase().contains(upperCaseName)) {
				keys.add("$$" + key + "$$");
			}
		}
		return new ArrayList<>(keys);
	}
}
