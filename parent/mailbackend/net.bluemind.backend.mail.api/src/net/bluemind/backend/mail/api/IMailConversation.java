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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.SortDescriptor;

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

	@POST
	@Path("_mget")
	public List<ItemValue<Conversation>> multipleGet(List<String> uids) throws ServerFault;

	/** Retrieve the conversations of the given folder. */
	@POST
	public List<String> byFolder(@QueryParam(value = "folder") String folderUid, SortDescriptor sorted);

	@DELETE
	@Path("{containerUid}/{itemId}/_message")
	public void removeMessage(@PathParam(value = "containerUid") String containerUid,
			@PathParam(value = "itemId") Long itemId);

}
