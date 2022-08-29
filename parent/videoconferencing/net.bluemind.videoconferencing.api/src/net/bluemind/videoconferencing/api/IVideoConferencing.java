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
package net.bluemind.videoconferencing.api;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@Path("/videoconferencing/{containerUid}")
public interface IVideoConferencing {

	@PUT
	public VEvent add(VEvent vevent);

	@DELETE
	public VEvent remove(VEvent vevent);

	public VEvent update(VEvent old, VEvent current);

	@PUT
	@Path("createResource/{uid}")
	public void createResource(@PathParam(value = "uid") String uid, VideoConferencingResourceDescriptor descriptor);
}
