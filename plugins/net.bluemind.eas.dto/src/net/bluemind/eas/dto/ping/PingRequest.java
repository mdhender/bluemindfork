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

public class PingRequest {

	public static final class Folders {
		public static final class Folder {

			public static enum Class {
				Email, Calendar, Contacts, Tasks, Notes;
			}

			public String id;
			public Class clazz;
		}

		public List<Folder> folders = new ArrayList<Folder>();
	}

	public Integer heartbeatInterval;
	public Folders folders = new Folders();

}
