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
package net.bluemind.system.api;

import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/configuration")
public interface ISystemConfiguration {

	/**
	 * Get server configuration
	 * 
	 * @return configuration values
	 * @throws ServerFault
	 */
	@GET
	public SystemConf getValues() throws ServerFault;

	@POST
	public void updateMutableValues(Map<String, String> values) throws ServerFault;

}
