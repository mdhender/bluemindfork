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

import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.backend.mail.api.flags.ConversationFlagUpdate;
import net.bluemind.backend.mail.api.flags.ImportMailboxConversationSet;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.ItemIdentifier;

/**
 * Handle message conversations for a given container (per user or mail-share).
 */
@BMApi(version = "3")
@Path("/mail_conversation/{conversationContainer}/{replicatedMailboxUid}")
public interface IMailConversationActions {

	/**
	 * Adds one flag to multiple
	 * {@link net.bluemind.backend.mail.api.Conversation}s.
	 * 
	 * @param flagUpdate
	 * @return the new container version
	 */
	@PUT
	@Path("_addFlag")
	Ack addFlag(ConversationFlagUpdate flagUpdate);

	/**
	 * Removes a flag from multiple
	 * {@link net.bluemind.backend.mail.api.Conversation}s.
	 * 
	 * @param flagUpdate
	 * @return the new container version
	 */
	@PUT
	@Path("_deleteFlag")
	Ack deleteFlag(ConversationFlagUpdate flagUpdate);

	/**
	 * Import conversations into folder.
	 * 
	 * @param folderDestinationId
	 * @param mailboxItems
	 * @return
	 * @throws ServerFault
	 */
	@PUT
	@Path("importItems/{folderDestinationId}")
	ImportMailboxItemsStatus importItems(@PathParam("folderDestinationId") long folderDestinationId,
			ImportMailboxConversationSet mailboxItems) throws ServerFault;

	/**
	 * Copy conversations.
	 * 
	 * @param targetMailboxUid
	 * @param conversationUids
	 * @return
	 */
	@POST
	@Path("copy/{targetMailboxUid}")
	List<ItemIdentifier> copy(@PathParam("targetMailboxUid") String targetMailboxUid, List<String> conversationUids);

	/**
	 * Move conversations.
	 * 
	 * @param targetMailboxUid
	 * @param conversationUids
	 * @return
	 */
	@POST
	@Path("move/{targetMailboxUid}")
	List<ItemIdentifier> move(@PathParam("targetMailboxUid") String targetMailboxUid, List<String> conversationUids);

}
