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

import java.util.Date;
import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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

	/**
	 * 
	 * @param uid:          message body guid (sha1 hexdump on the body content)
	 * @param deliveryDate: given by an imap client
	 * @param eml
	 */
	@PUT
	@Path("{uid}/_withdeliverydate")
	void createWithDeliveryDate(@PathParam("uid") String uid, Date deliveryDate, Stream eml);

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
