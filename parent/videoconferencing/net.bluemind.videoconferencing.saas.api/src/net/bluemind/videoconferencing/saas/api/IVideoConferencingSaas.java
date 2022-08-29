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
package net.bluemind.videoconferencing.saas.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@Path("/videoconferencing/saas")
public interface IVideoConferencingSaas {
	@POST
	@Path("createToken/{roomName}")
	public BlueMindVideoTokenResponse token(@PathParam(value = "roomName") String roomName);

	@POST
	@Path("createRoom")
	public BlueMindVideoRoom create(BlueMindVideoRoom room);

	@POST
	@Path("updateTitle/{roomName}")
	public void updateTitle(@PathParam(value = "roomName") String roomName, String title);

	@GET
	@Path("getRoom/{roomName}")
	public BlueMindVideoRoom get(@PathParam(value = "roomName") String roomName);
}
