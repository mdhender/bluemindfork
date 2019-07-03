/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.Stream;

/**
 * Database only version of {@link IMessageBodies} for sync server usage.
 *
 */
@BMApi(version = "3", internal = true)
@Path("/db_message_bodies/{partition}")
public interface IDbMessageBodies {

	@DELETE
	@Path("{uid}")
	void delete(@PathParam("uid") String uid);

	@PUT
	@Path("{uid}")
	void create(@PathParam("uid") String uid, Stream eml);

	@GET
	@Path("{uid}/complete")
	MessageBody getComplete(@PathParam("uid") String uid);

	@GET
	@Path("{uid}/exists")
	boolean exists(@PathParam("uid") String uid);

	@POST
	@Path("_missing")
	List<String> missing(List<String> toCheck);

	@POST
	@Path("_multiple")
	List<MessageBody> multiple(List<String> uid);

	@POST
	@Path("_update")
	void update(MessageBody body);
}
