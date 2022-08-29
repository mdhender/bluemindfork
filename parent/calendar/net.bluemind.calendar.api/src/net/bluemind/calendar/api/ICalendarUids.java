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
package net.bluemind.calendar.api;

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
 * Returns specific calendar container UIDs.
 */
@BMApi(version = "3")
@Path("/calendar/uids")
public interface ICalendarUids {

	public static final String TYPE = "calendar";

	public enum UserCalendarType {
		Default, UserCreated
	}

	/**
	 * Returns the UID of user-created calendars
	 * 
	 * @param uniqueUid
	 *                      unique calendar UID
	 * @return calendar UID
	 */
	@GET
	@Path("{uid}/_other_calendar")
	public default String getUserCreatedCalendar(@PathParam("uid") String uniqueUid) {
		return ICalendarUids.userCreatedCalendar(uniqueUid);
	}

	/**
	 * Returns the default user calendar UID
	 * 
	 * @param uid
	 *                the {@link net.bluemind.user.api.User} UID
	 * @return default user calendar UID
	 */
	@GET
	@Path("{uid}/_default_calendar")
	public default String getDefaultUserCalendar(@PathParam("uid") String userUid) {
		return ICalendarUids.defaultUserCalendar(userUid);
	}

	/**
	 * Returns the ressource calendar UIDs
	 * 
	 * @param uid
	 *                unique calendar UID
	 * @return ressource calendar UID
	 */
	@GET
	@Path("{uid}/_resource_calendar")
	public static String getResourceCalendar(@PathParam("uid") String uid) {
		return ICalendarUids.resourceCalendar(uid);
	}

	public static String userCreatedCalendar(String randomSeed) {
		return TYPE + ":" + UserCalendarType.UserCreated + ":" + randomSeed;
	}

	public static String defaultUserCalendar(String uid) {
		return TYPE + ":" + UserCalendarType.Default + ":" + uid;
	}

	public static String resourceCalendar(String uid) {
		return TYPE + ":" + uid;
	}

}
