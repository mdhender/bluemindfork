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
import java.util.Set;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3", internal = true)
@Path("/replicated_mailboxes")
public interface IReplicatedMailboxesMgmt {

	@GET
	@Path("{guid}/references")
	Set<MailboxRecordItemUri> getBodyGuidReferences(@PathParam("guid") String guid);

	@GET
	@Path("{mailbox}/{replicatedmailboxuid}/{uid}/references_byuid")
	List<Set<MailboxRecordItemUri>> getImapUidReferences(@PathParam("mailbox") String mailbox,
			@PathParam("replicatedmailboxuid") String replicatedMailboxUid, @PathParam("uid") Long uid);

	@GET
	@Path("{mailbox}/references/query")
	List<Set<MailboxRecordItemUri>> queryReferences(@PathParam("mailbox") String mailbox, String query);

	/**
	 * Resolves a list of cyrus mailboxes internal names
	 * 
	 * @param names
	 * @return
	 */
	@POST
	@Path("_mresolve")
	List<ResolvedMailbox> resolve(List<String> names);
}
