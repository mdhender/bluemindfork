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

import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import net.bluemind.backend.mail.api.flags.ConversationFlagUpdate;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.api.Ack;

/**
 * Handle message conversations for a given container (per user or mail-share).
 */
@BMApi(version = "3")
@Path("/mail_conversation/{conversationContainer}/{replicatedMailboxUid}")
public interface IMailConversationActions {

	/**
	 * Add one flag to multiple {@link net.bluemind.backend.mail.api.Conversation}.
	 * 
	 * @param flagUpdate
	 * @return the new container version
	 */
	@PUT
	@Path("_addFlag")
	Ack addFlag(ConversationFlagUpdate flagUpdate);

}
