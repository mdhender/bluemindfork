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

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
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

public class SqlScripts {

	private List<Updater> sqlScripts;
	private static final Logger logger = LoggerFactory.getLogger(SqlScripts.class);
	private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");

	public SqlScripts() {
		this.sqlScripts = new LinkedList<>();
		loadScripts();
	}

	private void loadScripts() {
		logger.debug("loading extensionpoint net.bluemind.core.jdbc");
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.system.schemaupgrader",
				"sqlscript");

		if (point == null) {
			logger.error("point net.bluemind.system.schemaupgrader: schema not found");
			throw new RuntimeException("point net.bluemind.system.schemaupgrader.sqlscripts name:schema not found");
		}
		IExtension[] extensions = point.getExtensions();
		for (IExtension ie : extensions) {
			Bundle bundle = Platform.getBundle(ie.getContributor().getName());
			logger.debug("loading scripts from bundle:{}", bundle.getSymbolicName());

			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (e.getName().equals("sql")) {

					String resource = e.getAttribute("script");
					URL url = bundle.getResource(resource);
					if (url == null) {
						logger.error("bundle [{}] resource {} not found", bundle.getSymbolicName(), resource);
						continue;
					}
					int sequence = Integer.parseInt(e.getAttribute("sequence"));
					Date date;
					try {
						date = df.parse(e.getAttribute("date_yyyyMMdd"));
					} catch (Exception ex) {
						throw new ServerFault("Cannot parse upgrader date", ex);
					}
					UpgraderDatabase db = null;
					try {
						db = (UpgraderDatabase) e.createExecutableExtension("database");
					} catch (CoreException e1) {
						logger.warn("Cannot read database attribute of sql upgrader PEXT: {}", e1.getMessage());
						db = new UpgraderDatabase.ALL();
					}
					boolean afterSchemaUpgrade = false;
					if (e.getAttribute("after_schema_upgrade") != null) {
						afterSchemaUpgrade = Boolean.parseBoolean(e.getAttribute("after_schema_upgrade"));
					}
					boolean ignoreErrors = Boolean.parseBoolean(e.getAttribute("ignore_errors"));
					Updater descriptor = new SqlUpdater(url, ignoreErrors, afterSchemaUpgrade, db.database(), date,
							sequence);
					sqlScripts.add(descriptor);
				}
			}

		}
	}

	public List<Updater> getSqlScripts() {
		return sqlScripts;
	}

}
