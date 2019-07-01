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
package net.bluemind.exchange.mapi.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;

@BMApi(version = "3")
@Path("/mapi/{domainUid}")
public interface IMapi {

	/**
	 * Returns a list of numerical identifiers usable as NSPI minimal ids
	 * 
	 * @param query
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_galQuery")
	List<Long> searchGAL(@QueryParam("query") String query) throws ServerFault;

	/**
	 * Fetches directory items from a list of NSPI minimalIds
	 * 
	 * @param galItems
	 * @return
	 * @throws ServerFault
	 */
	@GET
	@Path("_galContent")
	List<ItemValue<DirEntry>> getGALContent(List<Long> galItems) throws ServerFault;

}
