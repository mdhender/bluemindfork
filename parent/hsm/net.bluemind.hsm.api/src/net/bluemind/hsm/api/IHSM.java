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
package net.bluemind.hsm.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;

@BMApi(version = "3")
@Path("/hsm/{domainUid}")
public interface IHSM {

	/**
	 * Fetches the content of a message from low-tier storage
	 * 
	 * @param mailboxUid
	 * @param hsmId
	 * @throws ServerFault
	 */
	@GET
	@Path("_fetch/{mailboxUid}/{hsmId}")
	@Produces("application/octet-stream")
	// TODO we could use a stream
	public byte[] fetch(@PathParam(value = "mailboxUid") String mailboxUid, @PathParam(value = "hsmId") String hsmId)
			throws ServerFault;

	/**
	 * Get user archive size used in byte
	 * 
	 * @param mailboxUid
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_getSize/{mailboxUid}")
	double getSize(@PathParam(value = "mailboxUid") String mailboxUid) throws ServerFault;

	/**
	 * @param sourceMailboxUid
	 * @param destMailboxUid
	 * @param hsmIds
	 * @throws ServerFault
	 */
	@POST
	@Path("_copy/{sourceMailboxUid}/{destMailboxUid}")
	void copy(@PathParam(value = "sourceMailboxUid") String sourceMailboxUid,
			@PathParam(value = "destMailboxUid") String destMailboxUid, List<String> hsmIds) throws ServerFault;

	/**
	 * Moves messages to an upper tier of storage (eg. from archive to the user
	 * mailbox)
	 * 
	 * @param promote
	 * @throws ServerFault
	 */
	@POST
	@Path("_massPromote")
	public List<TierChangeResult> promoteMultiple(List<Promote> promote) throws ServerFault;
}
