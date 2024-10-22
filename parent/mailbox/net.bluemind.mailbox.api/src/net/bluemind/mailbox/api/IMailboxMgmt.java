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

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.server.api.Server;

@BMApi(version = "3")
@Path("/mgmt/mailbox/{domainUid}")
public interface IMailboxMgmt {

	/**
	 * consolidate a single mailbox alias
	 * 
	 * @param mailboxUid
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{mailboxUid}/_consolidate")
	public TaskRef consolidateMailbox(@PathParam("mailboxUid") String mailboxUid) throws ServerFault;

	/**
	 * reset a single mailbox alias
	 * 
	 * @param mailboxUid
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{mailboxUid}/_reset")
	public TaskRef resetMailbox(@PathParam("mailboxUid") String mailboxUid) throws ServerFault;

	/**
	 * respawn a single mailbox alias into a new shard
	 * 
	 * @param mailboxUid
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{mailboxUid}/_respawn")
	public TaskRef respawnMailbox(@PathParam("mailboxUid") String mailboxUid) throws ServerFault;

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
	 * @param indexName    indexName must start with mailspool. ex mailspool_2
	 * @param deleteSource
	 * @return
	 * @throws ServerFault
	 */
	@POST
	@Path("{mailboxUid}/_move_index")
	public TaskRef moveIndex(@PathParam("mailboxUid") String mailboxUid, @QueryParam("index") String indexName,
			@QueryParam("deleteSource") boolean deleteSource) throws ServerFault;

	@PUT
	@Path("{numericIndex}/_add_index")
	public TaskRef addIndexToRing(@PathParam("numericIndex") Integer numericIndex) throws ServerFault;

	@DELETE
	@Path("{numericIndex}/_remove_index")
	public TaskRef deleteIndexFromRing(@PathParam("numericIndex") Integer numericIndex) throws ServerFault;

	public void move(ItemValue<Mailbox> mailbox, ItemValue<Server> server) throws ServerFault;

	/**
	 * @return list of {@link ShardStats} ordered by {@link ShardStats#docCount}
	 */
	@GET
	@Path("shardsStats")
	public List<ShardStats> getShardsStats();

	/**
	 * @return list of {@link ShardStats} ordered by {@link ShardStats#docCount}
	 */
	@GET
	@Path("liteStats")
	public List<SimpleShardStats> getLiteStats();

}
