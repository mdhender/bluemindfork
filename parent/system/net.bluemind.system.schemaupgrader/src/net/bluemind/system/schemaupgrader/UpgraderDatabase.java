/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.system.schemaupgrader;

import net.bluemind.system.api.Database;

public interface UpgraderDatabase {

	public Database database();

	public static class DIRECTORY implements UpgraderDatabase {

		@Override
		public Database database() {
			return Database.DIRECTORY;
		}

	}

	public static class SHARD implements UpgraderDatabase {

		@Override
		public Database database() {
			return Database.SHARD;
		}

	}

	public static class ALL implements UpgraderDatabase {

		@Override
		public Database database() {
			return Database.ALL;
		}

	}

}
