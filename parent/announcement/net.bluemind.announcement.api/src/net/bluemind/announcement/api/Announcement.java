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
package net.bluemind.announcement.api;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class Announcement {

	@BMApi(version = "3")
	public enum Kind {
		Info, Warn, Error
	}

	@BMApi(version = "3")
	public enum Target {
		Admin, User, All
	}

	public Target target;
	public Kind kind;
	public String message;
	public boolean closeable;
	public Map<String, String> data;
	public String link;

	public static Announcement create(Target target, Kind kind, String message, boolean closeable) {
		Announcement ret = new Announcement();
		ret.target = target;
		ret.kind = kind;
		ret.message = message;
		ret.closeable = closeable;
		ret.data = new HashMap<String, String>();
		return ret;
	}

}
