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
package net.bluemind.tag.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import net.bluemind.core.api.BMApi;

/**
 * To unify the management of changelogs, ACLs, client synchronization,
 * permissions and sharing, Bluemind stores all elements in a generic structure
 * called a container. All containers are identified by a unique ID. Some
 * containers are named (UID) in a specific manner to express a certain meaning.
 * 
 * 
 * Returns specific tags container UIDs
 */
@BMApi(version = "3")
@Path("/tags/uids")
public interface ITagUids {
	public static final String TYPE = "tags";

	/**
	 * Returns the default user tag UID
	 * 
	 * @param userUid the {@link net.bluemind.user.api.User} UID
	 * @return default user tag UID
	 */
	@GET
	@Path("{uid}/_default_tags")
	public default String getDefaultUserTags(@PathParam("uid") String userUid) {
		return defaultTags(userUid);
	}

	public static String defaultTags(String uid) {
		return TYPE + "_" + uid;
	}
}
