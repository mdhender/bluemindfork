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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

public class UIExtensionsManager {

	private static UIExtensionsManager instance = new UIExtensionsManager();
	private JSONObject extensionPoints;

	public UIExtensionsManager() {
		JavaScriptObject extensions = loadExtensionPoints();
		extensionPoints = new JSONObject(extensions);

	}

	private native JavaScriptObject loadExtensionPoints()
	/*-{
	return $wnd["bmExtensions_"];
	}-*/;

	public UIExtensionPoint getExtensionPoint(String id) {
		if (extensionPoints == null) {
			return null;
		}

		JSONValue value = extensionPoints.get(id);

		if (value == null || value.isArray() == null) {
			return null;
		} else {
			return new UIExtensionPoint(id, value.isArray());
		}
	}

	public static UIExtensionsManager getInstance() {
		return instance;
	}
}
