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
public class LogEntry {

	public long timestamp;
	public LogLevel severity;
	public String locale;
	public String content;
	public int offset;

	@Override
	public boolean equals(Object obj) {
		LogEntry le = (LogEntry) obj;
		return (timestamp == le.timestamp && severity == le.severity && locale.equals(le.locale)
				&& content.equals(le.content) && offset == le.offset);
	}

	@Override
	public String toString() {
		return timestamp + " - " + severity + " - " + locale + " - " + content + " - " + offset;
	}

}
