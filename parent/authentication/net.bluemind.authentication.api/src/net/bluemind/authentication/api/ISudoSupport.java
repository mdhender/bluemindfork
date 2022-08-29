/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.authentication.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3", internal = true)
@Path("/auth/sudo_support")
public interface ISudoSupport {

	/**
	 * Overrides the owner of a session. The items created/updated with this session
	 * will be held by the given subject.
	 * 
	 * @param subject
	 */
	@POST
	@Path("_owner")
	void setOwner(@QueryParam("subject") String subject);

}
