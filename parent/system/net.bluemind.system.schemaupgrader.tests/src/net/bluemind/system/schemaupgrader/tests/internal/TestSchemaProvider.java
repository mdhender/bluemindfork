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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.schemaupgrader.DatedUpdater;
import net.bluemind.system.schemaupgrader.ISchemaUpgradersProvider;
import net.bluemind.system.schemaupgrader.JavaUpdater;
import net.bluemind.system.schemaupgrader.SqlScripts;
import net.bluemind.system.schemaupgrader.Updater;

public class TestSchemaProvider implements ISchemaUpgradersProvider {

	private static final Logger logger = LoggerFactory.getLogger(TestSchemaProvider.class);
	private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

	@Override
	public List<DatedUpdater> allJavaUpdaters() {
		List<DatedUpdater> upgraders = new ArrayList<>();
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.system.schemaupgrader",
				"javacode");

		if (point == null) {
			logger.error("point net.bluemind.system.schemaupgrader: javacode not found");
			throw new RuntimeException("point net.bluemind.system.schemaupgrader.sqlscripts name:schema not found");
		}
		IExtension[] extensions = point.getExtensions();
		for (IExtension ie : extensions) {
			Bundle bundle = Platform.getBundle(ie.getContributor().getName());
			logger.debug("loading scripts from bundle:{}", bundle.getSymbolicName());

			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (e.getName().equals("java")) {

					int sequence = Integer.parseInt(e.getAttribute("sequence"));
					Date date;
					try {
						date = df.parse(e.getAttribute("date_yyyyMMdd"));
					} catch (Exception ex) {
						throw new ServerFault("Cannot parse upgrader date", ex);
					}

					try {
						Updater updater = (Updater) e.createExecutableExtension("code");
						upgraders.add(new JavaUpdater(updater, date, sequence));
					} catch (CoreException e1) {
						throw new ServerFault(e1);
					}

				}
			}
		}
		return upgraders;
	}

	@Override
	public List<DatedUpdater> allSqlUpdaters() {
		return new SqlScripts().getSqlScripts();
	}

	@Override
	public boolean isActive() {
		return true;
	}

}
