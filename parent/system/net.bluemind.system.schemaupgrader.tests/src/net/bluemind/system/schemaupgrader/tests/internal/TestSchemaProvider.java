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
package net.bluemind.system.schemaupgrader.tests.internal;

import java.util.List;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.schemaupgrader.AtEveryUpgrade;
import net.bluemind.system.schemaupgrader.ISchemaUpgradersProvider;
import net.bluemind.system.schemaupgrader.SqlScripts;
import net.bluemind.system.schemaupgrader.Updater;

public class TestSchemaProvider implements ISchemaUpgradersProvider {

	@Override
	public List<Updater> allJavaUpdaters() {
		RunnableExtensionLoader<Updater> epLoader = new RunnableExtensionLoader<>();
		List<Updater> allJavaUpdaters = epLoader.loadExtensions("net.bluemind.system.schemaupgrader", "javacode",
				"java", "code");

		return allJavaUpdaters;
	}

	@Override
	public List<Updater> allSqlUpdaters() {
		return new SqlScripts().getSqlScripts();
	}

	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public List<AtEveryUpgrade> atEveryUpgradeJavaUpdaters() {
		RunnableExtensionLoader<AtEveryUpgrade> epLoader = new RunnableExtensionLoader<>();
		return epLoader.loadExtensionsWithPriority("net.bluemind.system.schemaupgrader", "ateveryupgrade", "java",
				"code");
	}

}
