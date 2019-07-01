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

import java.util.Collection;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public interface ICustomPropertiesRequirements {

	public String support();

	/**
	 * @return the ID of the plugin asking for custom properties
	 */
	public String getRequesterId();

	/**
	 * Get defined custom properties
	 * 
	 * @return defined custom properties
	 */
	public Collection<CustomProperty> getCustomProperties();

	/**
	 * Get custom property using its name
	 * 
	 * @param name
	 *            custom property name
	 * @return custom property definition
	 */
	public CustomProperty getByName(String name);
}
