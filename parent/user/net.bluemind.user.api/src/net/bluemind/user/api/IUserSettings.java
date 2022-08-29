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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

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
	
	/**
	 * Create or update one {@link User} setting
	 * 
	 * @param uid
	 *            uid of the user
	 * @param name
	 *            setting name
	 * @param value
	 *            setting value
	 * @throws ServerFault
	 */
	@POST
	@Path("{uid}/_setting/{name}")
	public void setOne(@PathParam(value = "uid") String uid,
			@PathParam(value = "name") String name, String value) throws ServerFault;

	/**
	 * Get one {@link User} setting value
	 * 
	 * @param uid
	 *            uid of the user
	 * @param name
	 * 				setting name
	 * @return String setting value
	 * @throws ServerFault
	 */
	@GET
	@Path("{uid}/_setting/{name}")
	public String getOne(@PathParam(value = "uid") String uid, @PathParam(value = "name") String name) throws ServerFault;
}
