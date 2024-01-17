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
package net.bluemind.eas.dto.ping;

import java.util.ArrayList;
import java.util.List;

public class PingResponse {

	public enum Status {

		NO_CHANGES(1), CHANGES_OCCURRED(2), MISSING_PARAMETER(3), SYNTAX_ERROR(4), //
		INVALID_HEARTBEAT_INTERVAL(5), TOO_MANY_FOLDERS(6), FOLDER_SYNC_REQUIRED(7), SERVER_ERROR(8);

		private final String xmlValue;

		private Status(int value) {
			xmlValue = Integer.toString(value);
		}

		public String xmlValue() {
			return xmlValue;
		}

	}

	public static final class Folders {
		public List<String> folders = new ArrayList<String>();
	}

	public Status status;
	public Folders folders;
	public Integer maxFolders;
	public Integer heartbeatInterval;

}
