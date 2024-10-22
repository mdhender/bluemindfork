/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.delivery.conversationreference.api;

import java.util.Set;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3", internal = true)
@Path("/conversationreferences/{domainUid}/{ownerUid}")
public interface IConversationReference {

	/**
	 * Lookups conversation ids using mail message-ID header and references header
	 *
	 * @param the message-ID
	 * @param the references
	 * @return the found conversation ID
	 * @throws ServerFault
	 */
	@POST
	@Path("lookup")
	Long lookup(@QueryParam("messageid") String messageId, Set<String> messageIdAndReferences) throws ServerFault;

}
