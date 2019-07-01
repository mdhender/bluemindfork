/*BEGIN LICENSE
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

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

/**
 * API for managing domain-wide settings. This {@link DomainSettingsKeys} enum
 * contains a non-exhaustive list of domain settings keys.
 * 
 */
@BMApi(version = "3")
@Path("/domains/{containerUid}")
public interface IDomainSettings {

	/**
	 * Define domain settings.
	 * 
	 * @param settings domain settings map
	 * @throws ServerFault standard error object
	 */
	@PUT
	@Path("_settings")
	public void set(Map<String, String> settings) throws ServerFault;

	/**
	 * Fetch domain settings.
	 * 
	 * @return domain settings map
	 * @throws ServerFault standard error object
	 */
	@GET
	@Path("_settings")
	public Map<String, String> get() throws ServerFault;
}
