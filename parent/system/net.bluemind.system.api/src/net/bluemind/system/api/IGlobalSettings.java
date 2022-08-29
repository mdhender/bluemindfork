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
package net.bluemind.system.api;

import java.util.Map;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.RequiredRoles;
import net.bluemind.core.api.fault.ServerFault;

/**
 * API for managing domain-wide settings.
 * 
 */
@BMApi(version = "3")
@Path("/global_settings")
public interface IGlobalSettings {
	/**
	 * Set global settings
	 * 
	 * @param settings
	 *            settings values
	 * @throws ServerFault
	 */
	@PUT
	@RequiredRoles("systemManagement")
	public void set(Map<String, String> settings) throws ServerFault;

	/**
	 * Get global settings
	 * 
	 * @return {@link ItemValue}&lt;Map&lt;String, String>> global settings
	 * @throws ServerFault
	 */
	@GET
	public Map<String, String> get() throws ServerFault;

	/**
	 * Delete a global settings value
	 * 
	 * @param key
	 *            key
	 * 
	 * @throws ServerFault
	 */
	@DELETE
	@RequiredRoles("systemManagement")
	public void delete(@QueryParam(value = "key") String key) throws ServerFault;
}
