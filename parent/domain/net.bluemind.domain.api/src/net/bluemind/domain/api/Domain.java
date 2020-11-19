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
package net.bluemind.domain.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Domain {

	/**
	 * A short text associated to your domain name
	 */
	public String label;

	/**
	 * Fully qualified domain name
	 */
	public String name;

	/**
	 * Description
	 */
	public String description;

	/**
	 * Custom properties
	 */
	public Map<String, String> properties = new HashMap<>();

	/**
	 * True, if this is the management domain global.virt
	 */
	public boolean global;

	/**
	 * Additional domain names pointing to this domain
	 */
	public Set<String> aliases = Collections.emptySet();

	/**
	 * Create a domain object.
	 * 
	 * @param name
	 * @param label
	 * @param description
	 * @param aliases
	 * @return a new domain object
	 */
	public static Domain create(String name, String label, String description, Set<String> aliases) {
		Domain ret = new Domain();
		ret.name = name;
		ret.label = label;
		ret.description = description;
		ret.aliases = aliases;
		return ret;
	}

	/**
	 * 
	 * Copy this domain object.
	 * 
	 * @return duplicated domain object
	 */
	public Domain copy() {
		Domain d = new Domain();
		d.label = label;
		d.name = name;
		d.description = description;
		d.properties = new HashMap<>(properties);
		d.global = global;
		d.aliases = new HashSet<>(aliases);
		return d;
	}

}
