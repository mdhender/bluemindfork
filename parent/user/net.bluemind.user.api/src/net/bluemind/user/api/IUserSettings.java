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
package net.bluemind.user.api;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/users/{containerUid}")
public interface IUserSettings {
	/**
	 * Set {@link User} settings
	 * 
	 * @param uid
	 *            uid of the user
	 * @param settings
	 *            settings values
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/_settings")
	public void set(@PathParam(value = "uid") String uid, Map<String, String> settings) throws ServerFault;

	/**
	 * Get {@link User} settings
	 * 
	 * @param uid
	 *            uid of the user
	 * @return &lt;Map&lt;String, String>> user settings
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/_settings")
	public Map<String, String> get(@PathParam(value = "uid") String uid) throws ServerFault;
}
