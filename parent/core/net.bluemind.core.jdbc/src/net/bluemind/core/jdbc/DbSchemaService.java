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
package net.bluemind.core.jdbc;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import net.bluemind.core.jdbc.persistance.DbSchemaStore;

public class DbSchemaService {

	private static final Logger logger = LoggerFactory.getLogger(DbSchemaService.class);

	private DbSchemaStore schemaStore;
	private boolean autocreate = false;

	private SchemaDescriptors schemas;
	private static long fullTime = 0;

	public DbSchemaService(DbSchemaStore schemaStore, boolean autocreate) {
		this.schemaStore = schemaStore;
		this.autocreate = autocreate;
		schemas = new SchemaDescriptors();
	}

	public void initialize() {
		initialize(true);
	}

	@VisibleForTesting
	public void initialize(boolean walEnabled) {
		long time = System.currentTimeMillis();
		initializeSchemas(walEnabled);
		long spentTime = (System.currentTimeMillis() - time);
		fullTime += spentTime;
		logger.info("initialize schema in {}ms (full time {})", spentTime, fullTime);
	}

	private void initializeSchemas(boolean walEnabled) {

		for (SchemaDescriptor descr : schemas.getDescriptors()) {
			initialize(descr, walEnabled);
		}

	}

	public SchemaDescriptor getSchemaDescriptor(String schemaId) {
		return schemas.getSchemaDescriptor(schemaId);
	}

	public void initializeSchema(String schemaId) {
		SchemaDescriptor schema = schemas.getSchemaDescriptor(schemaId);
		if (schema == null) {
			throw new RuntimeException("schema " + schemaId + " doesnt exists");
		}

		initialize(schema, true);
	}

	private void initialize(SchemaDescriptor descr, boolean walEnabled) {

		String version = schemaStore.getSchemaVersion(descr.getName());
		if (descr.getVersion().equals(version)) {
			logger.debug("schema " + descr.getName() + " already present");
		} else if (version == null) {
			logger.debug("schema " + descr.getName() + " not present, autocreate:" + autocreate);
			if (autocreate) {
				logger.debug("**** Running {}", descr.getId());
				try {
					schemaStore.createSchema(descr, walEnabled);
				} catch (JdbcException e) {
					if (!descr.isIgnoreErrors()) {
						throw e;
					}
				}
			} else {
				throw new RuntimeException("schema with id " + descr.getId() + " not found");
			}
		} else {
			throw new RuntimeException("schema with id " + descr.getId() + " in wrong version [present:" + version
					+ "] [expected:" + descr.getVersion() + "]");

		}

	}

	public static DbSchemaService getService(DataSource ds, boolean autocreateSchemas) {
		return new DbSchemaService(new DbSchemaStore(ds), autocreateSchemas);
	}

}
