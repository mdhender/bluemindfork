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
package net.bluemind.dataprotect.calendar.internal;

import java.util.Collections;
import java.util.List;

import net.bluemind.system.schemaupgrader.AtEveryUpgrade;
import net.bluemind.system.schemaupgrader.ISchemaUpgradersProvider;
import net.bluemind.system.schemaupgrader.Updater;

public class RestoreSchemaProvider implements ISchemaUpgradersProvider {

	@Override
	public List<Updater> allJavaUpdaters() {
		return Collections.emptyList();
	}

	@Override
	public List<Updater> allSqlUpdaters() {
		return Collections.emptyList();
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public List<AtEveryUpgrade> atEveryUpgradeJavaUpdaters() {
		return Collections.emptyList();
	}

}
