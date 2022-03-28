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

import java.util.Date;
import java.util.Set;

import javax.sql.DataSource;

import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.system.api.Database;

public class JavaUpdater implements DatedUpdater {

	private final Updater updater;
	private final Date date;
	private final int sequence;

	public JavaUpdater(Updater updater, Date date, int sequence) {
		this.updater = updater;
		this.date = date;
		this.sequence = sequence;
	}

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions) {
		return updater.executeUpdate(monitor, pool, handledActions);
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return updater.afterSchemaUpgrade();
	}

	@Override
	public Date date() {
		return date;
	}

	@Override
	public Database database() {
		return updater.database();
	}

	@Override
	public int sequence() {
		return sequence;
	}

	public String name() {
		return this.toString();
	}

	public String toString() {
		return DatedUpdater.super.name() + " Class: " + updater.getClass().getName();
	}

}
