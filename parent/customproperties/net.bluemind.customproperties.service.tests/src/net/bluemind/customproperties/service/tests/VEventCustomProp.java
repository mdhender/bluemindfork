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
package net.bluemind.customproperties.service.tests;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.bluemind.customproperties.api.CustomProperty;
import net.bluemind.customproperties.api.CustomPropertyType;
import net.bluemind.customproperties.api.ICustomPropertiesRequirements;

public class VEventCustomProp implements ICustomPropertiesRequirements {

	private Map<String, CustomProperty> props;

	public VEventCustomProp() {
		props = new HashMap<String, CustomProperty>();

		CustomProperty cp = null;

		cp = new CustomProperty();
		cp.name = "custom prop1";
		cp.type = CustomPropertyType.STRING;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "Custom prop1");
		cp.addNameTranslation("en", "Custom prop1");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "custom prop2";
		cp.type = CustomPropertyType.INT;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "Custom prop2");
		cp.addNameTranslation("en", "Custom prop2");
		props.put(cp.name, cp);

	}

	@Override
	public String support() {
		return "vevent";
	}

	@Override
	public String getRequesterId() {
		return "junit-requester-id";
	}

	@Override
	public Collection<CustomProperty> getCustomProperties() {
		return props.values();
	}

	@Override
	public CustomProperty getByName(String name) {
		return props.get(name);
	}

}
