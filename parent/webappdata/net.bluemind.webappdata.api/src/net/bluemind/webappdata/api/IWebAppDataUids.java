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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import net.bluemind.core.api.BMApi;

/**
 * To unify the management of changelogs, ACLs, client synchronization,
 * permissions and sharing, Bluemind stores all elements in a generic structure
 * called a container. All containers are identified by a unique ID. Some
 * containers are named (UID) in a specific manner to express a certain meaning.
 * 
 * 
 * Returns {@link net.bluemind.webappdata.api.WebAppData} container UID
 */
@BMApi(version = "3")
@Path("/webappdata/uids")
public interface IWebAppDataUids {

	public static final String TYPE = "webappdata";

	public static String containerUid(String userUid) {
		return TYPE + ":" + userUid;
	}

	/**
	 * Returns the {@link net.bluemind.webappdata.api.WebAppData} user container UID
	 * 
	 * @param uid the {@link net.bluemind.user.api.User} UID
	 * @return {@link net.bluemind.webappdata.api.WebAppData} user container UID
	 */
	@GET
	@Path("{userUid}/_container_uid")
	public default String getContainerUid(@PathParam("userUid") String userUid) {
		return containerUid(userUid);
	}

}
