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

import java.util.Collections;
import java.util.Map;

import net.bluemind.core.api.BMApi;
import net.bluemind.directory.api.DirEntry;

@BMApi(version = "3")
public class CalendarDescriptor {

	public String name;
	public String domainUid;
	public Map<String, String> settings = Collections.emptyMap();

	/**
	 * the optional expected itemId in flat hierarchy
	 */
	public Long expectedId;

	/**
	 * {@link DirEntry}
	 */
	public String owner;
	public String orgUnitUid;

	public static CalendarDescriptor create(String name, String owner, String domainUid) {
		CalendarDescriptor ab = new CalendarDescriptor();
		ab.name = name;
		ab.owner = owner;
		ab.domainUid = domainUid;
		return ab;
	}

}
