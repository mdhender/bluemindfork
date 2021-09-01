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
package net.bluemind.backend.mail.replica.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.backend.mail.api.Conversation;
import net.bluemind.backend.mail.api.IMailConversation;
import net.bluemind.core.api.BMApi;

/**
 * Handle message conversations for a given container (per user or mail-share).
 */
@BMApi(version = "3", internal = true)
@Path("/mail_conversation/{conversationContainer}")
public interface IInternalMailConversation extends IMailConversation {

	/** Create a new conversation. */
	@PUT
	@Path("{uid}")
	public void create(@PathParam(value = "uid") String uid, Conversation conversation);

	/** Update an existing conversation. */
	@POST
	@Path("{uid}")
	public void update(@PathParam(value = "uid") String uid, Conversation conversation);

	@DELETE
	@Path("{folderUid}")
	public void deleteAll(@PathParam(value = "folderUid") String folderUid);

}
