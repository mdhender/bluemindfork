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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.api.ICountingSupport;

@BMApi(version = "3", internal = true)
@Path("/record_expunged/{replicatedMailboxUid}")
public interface IMailboxRecordExpunged extends ICountingSupport {

	@POST
	@Path("_delete")
	public void delete(@QueryParam("id") long itemId);

	@POST
	@Path("_multipleDelete")
	public void multipleDelete(List<Long> itemIds);

	@GET
	@Path("_fetch")
	public List<MailboxRecordExpunged> fetch();

	@GET
	@Path("_get")
	public MailboxRecordExpunged get(@QueryParam("id") long itemId);

}
