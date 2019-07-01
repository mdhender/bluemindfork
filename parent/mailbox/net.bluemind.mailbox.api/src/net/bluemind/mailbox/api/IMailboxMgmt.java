/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.mailbox.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.server.api.Server;

@BMApi(version = "3")
@Path("/mgmt/mailbox/{domainUid}")
public interface IMailboxMgmt {

	/**
	 * consolidate a single mailbox index
	 * 
	 * @param mailboxUid
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{mailboxUid}/_consolidate")
	public TaskRef consolidateMailbox(@PathParam("mailboxUid") String mailboxUid) throws ServerFault;

	/**
	 * reindex all domain mailboxes
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("_consolidate")
	public TaskRef consolidateDomain() throws ServerFault;

	/**
	 * move ES index
	 * 
	 * @param mailboxUid
	 * @param indexName  indexName must start with mailspool. ex mailspool_2
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{mailboxUid}/_move_index")
	public TaskRef moveIndex(@PathParam("mailboxUid") String mailboxUid, @QueryParam("index") String indexName)
			throws ServerFault;

	public void move(ItemValue<Mailbox> mailbox, ItemValue<Server> server) throws ServerFault;

	/**
	 * @return list of {@link ShardStats} ordered by {@link ShardStats#docCount}
	 */
	@GET
	@Path("shardsStats")
	public List<ShardStats> getShardsStats();
}
