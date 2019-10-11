/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.exchange.mapi.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.api.IDataShardSupport;

@BMApi(version = "3")
@Path("/mapi_rule/{folderId}")
public interface IMapiRules extends IDataShardSupport {

	@PUT
	@Path("_mupdates")
	void updates(MapiRuleChanges changes);

	@GET
	List<MapiRule> all();

}
