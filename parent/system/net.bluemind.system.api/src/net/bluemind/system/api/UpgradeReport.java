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
package net.bluemind.system.api;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class UpgradeReport {

	public Status status;

	public List<UpgraderReport> upgraders = new ArrayList<>();

	@BMApi(version = "3")
	public static enum Status {
		FAILED, OK
	}

	@BMApi(version = "3")
	public static class UpgraderReport {

		public int major;

		public int build;

		public Status status;

		public static UpgraderReport create(int major, int build, Status status) {
			UpgraderReport ret = new UpgraderReport();
			ret.major = major;
			ret.build = build;
			ret.status = status;
			return ret;
		}
	}

}
