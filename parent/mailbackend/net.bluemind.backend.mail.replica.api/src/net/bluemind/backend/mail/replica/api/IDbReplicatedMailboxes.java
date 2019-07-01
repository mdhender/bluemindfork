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

import net.bluemind.backend.mail.api.IBaseMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.model.ItemValue;

/**
 * Database only version of {@link IMailboxFolders} for sync server usage.
 */
@BMApi(version = "3", internal = true)
@Path("/db_replicated_mailboxes/{partition}/{mailboxRoot}")
public interface IDbReplicatedMailboxes extends IBaseMailboxFolders, IDataShardSupport {

	@PUT
	@Path("{uid}")
	void create(@PathParam("uid") String uid, MailboxReplica replica);

	@POST
	@Path("{uid}")
	void update(@PathParam("uid") String uid, MailboxReplica replica);

	@DELETE
	@Path("{uid}")
	void delete(@PathParam("uid") String uid);

	@GET
	@Path("byReplicaName/{name}")
	ItemValue<MailboxReplica> byReplicaName(@PathParam("name") String name);

	@GET
	@Path("_allReplicas")
	List<ItemValue<MailboxReplica>> allReplicas();

}
