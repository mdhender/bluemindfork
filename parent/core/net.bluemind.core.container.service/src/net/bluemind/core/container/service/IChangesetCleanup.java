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
package net.bluemind.core.container.service;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3", internal = true)
@Path("/changeset_cleanup/{serverUid}")
public interface IChangesetCleanup {

	@POST
	@Path("_delete_old_changeset_items")
	public void deleteOldDeletedChangesetItems(int days);

}