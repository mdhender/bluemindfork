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
package net.bluemind.samba.customproperties;

import java.util.Collection;
import java.util.HashMap;

import net.bluemind.customproperties.api.CustomProperty;
import net.bluemind.customproperties.api.CustomPropertyType;
import net.bluemind.customproperties.api.ICustomPropertiesRequirements;

public class SambaGroupCustomProperties extends SambaCustomProperties implements ICustomPropertiesRequirements {

	public static final String SUPPORT = "group";

	private final HashMap<String, CustomProperty> props;

	public SambaGroupCustomProperties() {
		props = new HashMap<String, CustomProperty>();

		CustomProperty cp = null;

		cp = new CustomProperty();
		cp.name = "gid";
		cp.type = CustomPropertyType.INT;
		cp.addNameTranslation("fr", "GID");
		cp.addNameTranslation("en", "GID");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "samba_enabled";
		cp.type = CustomPropertyType.BOOLEAN;
		cp.addNameTranslation("fr", "Groupe Windows");
		cp.addNameTranslation("en", "Windows group");
		props.put(cp.name, cp);
	}

	@Override
	public String support() {
		return SUPPORT;
	}

	@Override
	public String getRequesterId() {
		return REQUESTER;
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
