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

import javax.sql.DataSource;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.schemaupgrader.ISchemaUpgradersProvider;
import net.bluemind.system.schemaupgrader.IVersionedUpdater;
import net.bluemind.system.schemaupgrader.Updater;
import net.bluemind.system.schemaupgrader.internal.SqlScripts;

public class TestSchemaProvider implements ISchemaUpgradersProvider {

	@Override
	public List<IVersionedUpdater> allJavaUpdaters(DataSource dataSource) {
		RunnableExtensionLoader<IVersionedUpdater> epLoader = new RunnableExtensionLoader<>();
		List<IVersionedUpdater> allJavaUpdaters = epLoader.loadExtensions("net.bluemind.system.schemaupgrader",
				"javacode", "java", "code");

		return allJavaUpdaters;
	}

	@Override
	public List<Updater> allSqlUpdaters(DataSource dataSource) {
		return new SqlScripts(dataSource).getSqlScripts();
	}

	@Override
	public boolean isActive() {
		return true;
	}

}
