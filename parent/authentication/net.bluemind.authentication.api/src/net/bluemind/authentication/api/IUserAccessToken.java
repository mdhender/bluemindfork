/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@Path("/auth/access_token")
public interface IUserAccessToken {

	@GET
	@Path("_info")
	public AccessTokenInfo getTokenInfo(@QueryParam("external_system") String externalSystem);

	@GET
	@Path("_auth")
	public AccessTokenInfo authCodeReceived(@QueryParam("state") String state, @QueryParam("code") String code);

}
