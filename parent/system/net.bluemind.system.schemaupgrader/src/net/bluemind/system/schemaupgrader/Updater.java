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
package net.bluemind.system.schemaupgrader;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import javax.sql.DataSource;

import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.system.api.Database;

public interface Updater {

	UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions);

	public Date date();

	public int sequence();

	public default Database database() {
		return Database.DIRECTORY;
	}

	boolean afterSchemaUpgrade();

	public default String name() {
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd");
		return formater.format(date()) + "-" + sequence() + "@" + database().name();
	}

}
