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
package net.bluemind.system.schemaupgrader.internal;

import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.system.schemaupgrader.IVersionedUpdater;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;
import net.bluemind.system.schemaupgrader.Updater;

public class ClassUpdater implements Updater {

	private IVersionedUpdater updater;
	private DataSource pool;
	private final String component;

	protected static final Logger logger = LoggerFactory.getLogger(ClassUpdater.class);

	public ClassUpdater(DataSource pool, IVersionedUpdater updater, String component) {
		this.pool = pool;
		this.updater = updater;
		this.component = component;
	}

	@Override
	public UpdateResult update(IServerTaskMonitor monitor, Set<UpdateAction> handledActions) {
		UpdateResult result = UpdateResult.noop();
		try {
			UpdateResult ret = updater.executeUpdate(monitor, pool, handledActions);
			monitor.log(String.format("Executed update on %s with result %s", updater.getClass().getName(),
					ret.result.name()));
			if (result != UpdateResult.failed()) {
				result = ret;
			} else {
				return result;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			monitor.log(e.getMessage());
			result = UpdateResult.failed();
		}
		return result;
	}

	@Override
	public int major() {
		return updater.major();
	}

	@Override
	public int build() {
		return updater.buildNumber();
	}

	public String toString() {
		return "JAVA v" + major() + "." + build() + " @ " + updater.getClass().getCanonicalName();
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return updater.afterSchemaUpgrade();
	}

	@Override
	public String getComponent() {
		return component;
	}
}
