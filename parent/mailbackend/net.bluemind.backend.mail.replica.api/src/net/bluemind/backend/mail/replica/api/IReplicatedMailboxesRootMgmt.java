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

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.MailboxReplicaRootUpdate;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3", internal = true)
@Path("/replicated_mailboxes_roots/{partition}")
public interface IReplicatedMailboxesRootMgmt {

	/**
	 * Create the folders subtree container for the replication of a mailbox. This
	 * can be called safely if the existence of the container is unknown.
	 * 
	 * @param root
	 */
	@PUT
	@Path("_create")
	void create(MailboxReplicaRootDescriptor root);

	/**
	 * For fucking rename as uid is not used in cyrus today
	 * 
	 * @param rename
	 */
	@POST
	@Path("_update")
	void update(MailboxReplicaRootUpdate rename);

	@DELETE
	@Path("_delete/{namespace}/{mailboxName}")
	void delete(@PathParam(value = "namespace") String namespace, @PathParam(value = "mailboxName") String mailboxName);

}
