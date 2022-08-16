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
package net.bluemind.webappdata.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.api.IChangelogSupport;
import net.bluemind.core.container.api.ICrudSupport;
import net.bluemind.core.container.api.IDataShardSupport;
import net.bluemind.core.container.api.IReadByIdSupport;
import net.bluemind.core.container.api.IRestoreCrudSupport;
import net.bluemind.core.container.model.ItemValue;

/**
 * 
 * WebAppData API - allow to save web applications data. All methods work on
 * {@link WebAppData} in a specific container identified by a unique UID, see
 * {@link WebAppData.getContainerUid}. Use
 * {@link net.bluemind.core.container.api.IContainers#all} to lookup all
 * containers of specific type.
 * 
 */
@BMApi(version = "3", genericType = WebAppData.class)
@Path("/webappdata/{containerUid}")
public interface IWebAppData extends IChangelogSupport, IDataShardSupport, ICrudSupport<WebAppData>,
		IRestoreCrudSupport<WebAppData>, IReadByIdSupport<WebAppData> {

	@GET
	@Path("key/{key}")
	WebAppData getByKey(@PathParam("key") String key);

	@GET
	@Path("_alluids")
	public List<String> allUids();

	@POST
	@Path("uid/{uid}")
	Ack update(@PathParam("uid") String uid, WebAppData value);

	@PUT
	@Path("uid/{uid}")
	Ack create(@PathParam("uid") String uid, WebAppData value);

	@DELETE
	@Path("uid/{uid}")
	void delete(@PathParam("uid") String uid);

	@DELETE
	@Path("_deleteAll")
	void deleteAll();

	@GET
	@Path("uid/{uid}")
	ItemValue<WebAppData> getComplete(@PathParam("uid") String uid);

	@POST
	@Path("uid/_mget")
	List<ItemValue<WebAppData>> multipleGet(List<String> uids);

}
