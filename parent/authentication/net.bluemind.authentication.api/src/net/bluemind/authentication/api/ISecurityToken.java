/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.authentication.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;

/**
 * This API allows upgrading short-lived session identifiers to persisting
 * tokens lasting 7 days.
 * 
 * Upgraded session identifiers do not require a
 * {@link IAuthentication#login(String, String, String)} call
 *
 */
@BMApi(version = "3", internal = true)
@Path("/auth/token/{sessionIdentifier}")
public interface ISecurityToken {

	/**
	 * Promote an existing core session to a long-lived access token
	 */
	@PUT
	@Path("_upgrade")
	void upgrade();

	/**
	 * Extend token life by 7 days
	 */
	@POST
	@Path("_renew")
	void renew();

	@DELETE
	@Path("_delete")
	void destroy();

}
