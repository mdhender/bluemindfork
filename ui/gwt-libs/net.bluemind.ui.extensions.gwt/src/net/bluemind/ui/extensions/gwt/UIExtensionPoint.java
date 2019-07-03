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

public class UIExtensionPoint {

	private String id;
	private UIExtension[] extensions;

	public UIExtensionPoint(String id, JSONArray extensionsArray) {
		this.id = id;
		this.extensions = new UIExtension[extensionsArray.size()];

		for (int i = 0; i < extensionsArray.size(); i++) {
			extensions[i] = new UIExtension(extensionsArray.get(i).isObject());
		}
	}

	public String getIdentifier() {
		return id;
	}

	public UIExtension[] getExtensions() {
		return extensions;
	}
}
