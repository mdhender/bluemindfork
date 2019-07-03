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

public class SambaUserCustomProperties extends SambaCustomProperties implements ICustomPropertiesRequirements {

	public static final String SUPPORT = "group";

	private final HashMap<String, CustomProperty> props;

	public SambaUserCustomProperties() {
		props = new HashMap<String, CustomProperty>();

		CustomProperty cp = null;

		cp = new CustomProperty();
		cp.name = "uid";
		cp.type = CustomPropertyType.INT;
		cp.addNameTranslation("fr", "uid");
		cp.addNameTranslation("en", "uid");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "system_group";
		cp.type = CustomPropertyType.GROUP;
		cp.addNameTranslation("fr", "Groupe principal");
		cp.addNameTranslation("en", "Main group");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "samba_enabled";
		cp.type = CustomPropertyType.BOOLEAN;
		cp.addNameTranslation("fr", "Utilisateur Windows");
		cp.addNameTranslation("en", "Windows user");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "home";
		cp.type = CustomPropertyType.STRING;
		cp.size = 255;
		cp.addNameTranslation("fr", "Chemin du dossier personnel");
		cp.addNameTranslation("en", "Home path");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "samba_home_drive";
		cp.type = CustomPropertyType.STRING;
		cp.size = 255;
		cp.addNameTranslation("fr", "Serveur du dossier personnel");
		cp.addNameTranslation("en", "Samba Home drive");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "drive_letter";
		cp.type = CustomPropertyType.STRING;
		cp.size = 2;
		cp.addNameTranslation("fr", "Lettre de lecteur");
		cp.addNameTranslation("en", "Drive letter");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "logon_script";
		cp.type = CustomPropertyType.STRING;
		cp.size = 255;
		cp.addNameTranslation("fr", "Script de login");
		cp.addNameTranslation("en", "Logon script");
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
