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
package net.bluemind.backend.mail.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;

/**
 * Handle message conversations for a given container (per user or mail-share).
 */
@BMApi(version = "3")
@Path("/mail_conversation/{conversationContainer}")
public interface IMailConversation {

	/** Retrieve the conversation having the given Cyrus identifier. */
	@GET
	@Path("{uid}")
	public ItemValue<Conversation> getComplete(@PathParam(value = "uid") String uid);

	/** Retrieve the conversations of the given folder. */
	@POST
	public List<ItemValue<Conversation>> byFolder(@QueryParam(value = "folder") String folderUid,
			ItemFlagFilter filter);

	@DELETE
	@Path("{containerUid}/{itemId}/_message")
	public void removeMessage(@PathParam(value = "containerUid") String containerUid,
			@PathParam(value = "itemId") Long itemId);

}
