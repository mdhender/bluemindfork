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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.core.task.service.IServerTaskMonitor;

public class SqlUpdater implements Updater {
	private static final Logger logger = LoggerFactory.getLogger(SqlUpdater.class);

	public final URL file;
	private final DataSource pool;
	private final int major;
	private final int build;
	private final boolean ignoreErrors;
	private final String component;
	private final boolean afterSchemaUpgrade;

	public SqlUpdater(DataSource pool, URL url, int major, int build, boolean ignoreErrors, String component,
			boolean afterSchemaUpgrade) {
		this.pool = pool;
		this.file = url;
		this.major = major;
		this.build = build;
		this.ignoreErrors = ignoreErrors;
		this.component = component;
		this.afterSchemaUpgrade = afterSchemaUpgrade;
	}

	public SqlUpdater(DataSource pool, URL url, int major, int build, boolean ignoreErrors, String component) {
		this(pool, url, major, build, ignoreErrors, component, false);
	}

	@Override
	public UpdateResult update(IServerTaskMonitor monitor, Set<UpdateAction> handledActions) throws Exception {
		monitor.log("On SQL script " + file.toString());
		String schemaValue = null;
		try (InputStream in = file.openStream()) {
			byte[] b = ByteStreams.toByteArray(in);
			schemaValue = new String(b);
		} catch (IOException e) {
			logger.error("error during script reading {}", file);
			monitor.log(e.getMessage());
			throw new RuntimeException(e);
		}

		try (Connection con = pool.getConnection(); Statement st = con.createStatement()) {
			st.execute(schemaValue);
		} catch (Exception e) {
			monitor.log(e.getMessage());
			logger.error("error during execution of script " + file, e);
			if (!ignoreErrors) {
				throw e;
			}
		}

		return UpdateResult.noop();
	}

	@Override
	public int major() {
		return major;
	}

	@Override
	public int build() {
		return build;
	}

	public String toString() {
		return "SQL script (ignoreErrors: " + ignoreErrors + ") v" + major + "." + build + " @ " + file.toString();
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return afterSchemaUpgrade;
	}

	@Override
	public String getComponent() {
		return component;
	}
}
