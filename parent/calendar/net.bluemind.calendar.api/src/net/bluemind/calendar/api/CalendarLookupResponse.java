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

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class CalendarLookupResponse {

	public String uid;
	public String name;
	public Type type;
	public String email;
	public int memberCount;
	public String ownerUid;

	@BMApi(version = "3")
	public enum Type {
		calendar, group
	}

	public static CalendarLookupResponse calendar(String uid, String name, String email, String ownerUid) {
		CalendarLookupResponse ret = new CalendarLookupResponse();
		ret.type = Type.calendar;
		ret.name = name;
		ret.uid = uid;
		ret.email = email;
		ret.ownerUid = ownerUid;
		return ret;
	}

}
