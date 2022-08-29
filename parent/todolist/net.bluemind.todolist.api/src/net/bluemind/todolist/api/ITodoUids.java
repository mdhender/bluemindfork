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
package net.bluemind.todolist.api;

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
 * Returns specific todolist container UIDs
 */
@BMApi(version = "3")
@Path("/todolist/uids")
public interface ITodoUids {

	public static final String TYPE = "todolist";

	/**
	 * Returns the default user todolist UID
	 * 
	 * @param uid
	 *                the {@link net.bluemind.user.api.User} UID
	 * @return default user todolist UID
	 */
	@GET
	@Path("{uid}/_default_todolist")
	public default String getDefaultUserTodoList(@PathParam("uid") String uid) {
		return defaultUserTodoList(uid);
	}

	/**
	 * Returns the UID of user-created todolists
	 * 
	 * @param uniqueUid
	 *                      unique todolist UID
	 * @return calendar UID
	 */
	@GET
	@Path("{uid}/_other_todolist")
	public default String getUserCreatedTodoList(@PathParam("uid") String uniqueUid) {
		return userCreatedTodoList(uniqueUid);
	}

	public static String defaultUserTodoList(String uid) {
		return TYPE + ":default_" + uid;
	}

	public static String userCreatedTodoList(String seed) {
		return TYPE + ":user_" + seed;
	}
}
