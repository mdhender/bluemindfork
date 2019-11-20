/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.metrics.core.db;

import java.sql.Connection;

import javax.sql.DataSource;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.Verticle;

import com.netflix.spectator.api.Registry;

import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.network.topology.Topology;

public class DbCheck extends Verticle implements BundleActivator {

	private Registry metricRegistry;
	private IdFactory idFactory;

	Logger logger = LoggerFactory.getLogger(DbCheck.class);

	@Override
	public void start() {
		super.start();

		metricRegistry = MetricsRegistry.get();
		idFactory = new IdFactory("jdbc", metricRegistry, DbCheck.class);

		super.vertx.setPeriodic(1000 * 10, (id) -> {
			Topology.getIfAvailable().ifPresent(topo -> {
				String coreuid = topo.core().uid;
				check(JdbcActivator.getInstance().getDataSource(), coreuid, "directory");
				JdbcActivator.getInstance().getMailboxDataSource().entrySet().forEach(e -> {
					check(e.getValue(), e.getKey(), "mailbox");
				});
			});
		});
	}

	private void check(DataSource ds, String dataLocation, String kind) {
		boolean isValid = false;
		try (Connection con = ds.getConnection()) {
			isValid = con.isValid(3);
		} catch (Exception e) {
			logger.warn("DBCheck failed on db {}", dataLocation, e);
		}

		if (!isValid) {
			logger.warn("DB-check failed on {}, {}", dataLocation, kind);
		}

		metricRegistry.gauge(idFactory.name("db-connections", "targetdatalocation", dataLocation, "dbkind", kind))
				.set(isValid ? 1 : 0);
	}

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new DbCheck();
		}

	}

	@Override
	public void start(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// TODO Auto-generated method stub

	}

}
