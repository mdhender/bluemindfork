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
import java.util.Date;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.system.api.Database;

public class SqlUpdater implements DatedUpdater {
	private static final Logger logger = LoggerFactory.getLogger(SqlUpdater.class);

	public final URL file;
	private final boolean ignoreErrors;
	private final boolean afterSchemaUpgrade;
	private final Database database;
	private final Date date;
	private final int sequence;

	public SqlUpdater(URL url, boolean ignoreErrors, boolean afterSchemaUpgrade, Database database, Date date,
			int sequence) {
		this.file = url;
		this.ignoreErrors = ignoreErrors;
		this.afterSchemaUpgrade = afterSchemaUpgrade;
		this.database = database;
		this.date = date;
		this.sequence = sequence;
	}

	public String name() {
		return this.toString();
	}

	public String toString() {
		return DatedUpdater.super.name() + " SQL:" + this.file.getFile();
	}

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions) {
		logger.info("executeupdate on {}: {}", pool, file.toString());
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
			con.setAutoCommit(false);
			try {
				st.execute(schemaValue);
				con.commit();
				return UpdateResult.ok();
			} catch (Exception e) {
				con.rollback();
				throw e;
			} finally {
				con.setAutoCommit(true);
			}
		} catch (Exception e) {
			monitor.log(e.getMessage());
			logger.error("error during execution of script {}", file, e);
			if (!ignoreErrors) {
				throw new ServerFault(e);
			}
		}

		return UpdateResult.noop();
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return afterSchemaUpgrade;
	}

	@Override
	public Date date() {
		return date;
	}

	@Override
	public int sequence() {
		return sequence;
	}

	@Override
	public Database database() {
		return database;
	}

}
