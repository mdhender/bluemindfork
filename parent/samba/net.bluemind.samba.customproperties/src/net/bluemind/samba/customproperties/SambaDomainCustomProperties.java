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

public class SambaDomainCustomProperties extends SambaCustomProperties implements ICustomPropertiesRequirements {

	public static final String SUPPORT = "domain";

	private final HashMap<String, CustomProperty> props;

	public SambaDomainCustomProperties() {
		props = new HashMap<String, CustomProperty>();

		CustomProperty cp = null;

		cp = new CustomProperty();
		cp.name = "samba_enabled";
		cp.type = CustomPropertyType.BOOLEAN;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "Domaine Windows");
		cp.addNameTranslation("en", "Windows Domain");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "sdn";
		cp.type = CustomPropertyType.STRING;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "Nom du domaine");
		cp.addNameTranslation("en", "Domain name");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "next_uid";
		cp.type = CustomPropertyType.INT;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "Prochain UID/GID utilisable");
		cp.addNameTranslation("en", "Next usable UID/GID");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "users_group";
		cp.type = CustomPropertyType.GROUP;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "Utilisateurs du domaine");
		cp.addNameTranslation("en", "Domain users");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "admins_group";
		cp.type = CustomPropertyType.GROUP;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "Administrateurs du domaine");
		cp.addNameTranslation("en", "Domain admins");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "guests_group";
		cp.type = CustomPropertyType.GROUP;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "Invités du domaine");
		cp.addNameTranslation("en", "Domain guests");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "profile_path";
		cp.type = CustomPropertyType.STRING;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "Serveur de profil (%u: login)");
		cp.addNameTranslation("en", "Profile server (%u: login)");
		props.put(cp.name, cp);

		cp = new CustomProperty();
		cp.name = "samba_sid";
		cp.type = CustomPropertyType.STRING;
		cp.globalAdminOnly = true;
		cp.addNameTranslation("fr", "SID du domaine");
		cp.addNameTranslation("en", "Domain SID");
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
