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
package net.bluemind.customproperties.api;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class CustomProperty {

	public String name;
	public CustomPropertyType type;
	public int size;
	public String defaultValue;
	public boolean globalAdminOnly = false;
	public Map<String, String> translatedName;

	public CustomProperty() {
		translatedName = new HashMap<String, String>();
	}

	public void addNameTranslation(String locale, String value) {
		translatedName.put(locale, value);
	}

}
