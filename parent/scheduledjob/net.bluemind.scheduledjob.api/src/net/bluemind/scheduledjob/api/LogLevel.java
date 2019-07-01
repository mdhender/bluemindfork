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
package net.bluemind.scheduledjob.api;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public enum LogLevel {
	/**
	 * Progress report percentage
	 */
	PROGRESS(-1),

	/**
	 * Logs from this severity can be ignored. They are just here to inform of
	 * what the job is doing
	 */
	INFO(0),

	/**
	 * Something un-expected happended.
	 */
	WARNING(1),

	/**
	 * Something we could not recover from happened.
	 */
	ERROR(2);

	private int lvl;

	private LogLevel(int lvl) {
		this.lvl = lvl;
	}

	/**
	 * Used for ordering of severities
	 * 
	 * @return
	 */
	public int getOrdering() {
		return lvl;
	}
}
