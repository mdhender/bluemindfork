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
package net.bluemind.backend.mail.api;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.model.ItemValue;

@BMApi(version = "3")
public interface IBaseMailboxFolders extends IChangelogSupport {

	/**
	 * @return INBOX for user mailboxes or the toplevel mailshare folder
	 */
	@GET
	@Path("_root")
	ItemValue<MailboxFolder> root();

	@GET
	@Path("byName/{name}")
	ItemValue<MailboxFolder> byName(@PathParam("name") String name);

	@GET
	@Path("{uid}/complete")
	ItemValue<MailboxFolder> getComplete(@PathParam("uid") String uid);

	@GET
	@Path("_all")
	List<ItemValue<MailboxFolder>> all();

	@POST
	@Path("_search")
	public SearchResult searchItems(MailboxFolderSearchQuery query);

}
