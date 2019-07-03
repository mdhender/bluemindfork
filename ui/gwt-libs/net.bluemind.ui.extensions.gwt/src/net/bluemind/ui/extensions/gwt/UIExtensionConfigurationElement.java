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
package net.bluemind.ui.extensions.gwt;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Window;

public class UIExtensionConfigurationElement {

	private JSONObject data;

	public UIExtensionConfigurationElement(JSONObject jsonValue) {
		this.data = jsonValue;
	}

	public UIExtensionConfigurationElement[] getConfigurationElements(String name) {
		JSONValue value = data.get(name);
		if (value == null) {
			return new UIExtensionConfigurationElement[0];
		} else if (value.isArray() != null) {
			Window.alert("found conf element for " + name);
			JSONArray array = value.isArray();
			UIExtensionConfigurationElement[] ret = new UIExtensionConfigurationElement[array.size()];
			for (int i = 0; i < array.size(); i++) {
				ret[i] = new UIExtensionConfigurationElement(array.get(i).isObject());
			}
			return ret;
		} else if (value.isObject() != null) {
			JSONObject o = value.isObject();
			UIExtensionConfigurationElement[] ret = new UIExtensionConfigurationElement[] {
					new UIExtensionConfigurationElement(o) };
			return ret;
		} else {
			return new UIExtensionConfigurationElement[0];
		}

	}

	public String getAttribute(String name) {

		JSONValue value = data.get(name);
		if (value == null || value.isString() == null) {
			return null;
		} else {
			return value.isString().stringValue();
		}
	}
}
