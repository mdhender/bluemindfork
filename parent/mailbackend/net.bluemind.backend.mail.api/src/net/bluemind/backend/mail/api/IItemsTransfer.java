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

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.model.ItemIdentifier;

/**
 * Transfer items between mailboxes that might live on separate backends
 */
@BMApi(version = "3")
@Path("/mail_items_transfer/{fromMailboxUid}/{toMailboxUid}")
public interface IItemsTransfer {

	@POST
	@Path("copy")
	List<ItemIdentifier> copy(List<Long> itemIds);

	@POST
	@Path("move")
	List<ItemIdentifier> move(List<Long> itemIds);

}
