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
package net.bluemind.webmodule.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsEntry {

	private static final Logger logger = LoggerFactory.getLogger(JsEntry.class);
	public final String path;
	public final boolean lifecycle;
	private boolean translation;
	private String bundle;
	private Map<String, String> translations;
	private List<String> dependencies = Collections.emptyList();

	public JsEntry(String path, boolean lifecycle, boolean translation) {
		this.path = path;
		this.lifecycle = lifecycle;
		this.translation = translation;
	}

	public String getBundle() {
		return bundle;
	}

	public String getPath() {
		return path;
	}
	
	public List<String> getDependencies() {
		return dependencies;
	}
	
	public void setDependencies(List<String> dependencies) {
		this.dependencies = dependencies;
	}
	
	public void setTranslations(Map<String, String> translations) {
		this.translations = translations;
	}
	
	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	public boolean isLifecycle() {
		return lifecycle;
	}

	public boolean hasTranslation() {
		return translation;
	}

	public JsEntry getTranslation(String lang) {
		if (!translation) {
			return this;
		}
		String p = translations.get(lang);
		if (p != null) {
			JsEntry ret = new JsEntry(p, lifecycle, translation);
			ret.translations = translations;
			ret.bundle = bundle;
			return ret;
		} else {
			logger.debug("no translation found for {} in {}", path, lang);
			return this;
		}
	}

}
