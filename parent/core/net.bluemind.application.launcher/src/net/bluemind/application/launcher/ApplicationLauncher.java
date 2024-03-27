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
package net.bluemind.application.launcher;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import net.bluemind.config.InstallationId;
import net.bluemind.configfile.ReloadableConfig;
import net.bluemind.core.api.BMVersion;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.jna.utils.MemlockSupport;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.metrics.annotations.TimeRangeAnnotation;
import net.bluemind.pool.BMPoolActivator;
import net.bluemind.pool.Pool;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.application.registration.model.ApplicationInfo;
import net.bluemind.system.state.StateContext;
import net.bluemind.system.validation.ProductChecks;
import net.bluemind.systemd.notify.Startup;
import sun.misc.Signal;

public class ApplicationLauncher implements IApplication {

	static {
		// HOLLOW uses JUL...
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	private static final Logger logger = LoggerFactory.getLogger(ApplicationLauncher.class);

	@Override
	public Object start(IApplicationContext context) throws Exception {
		logger.info("Starting BlueMind Application...");

		Signal.handle(new Signal("HUP"),
				signal -> VertxPlatform.eventBus().publish(ReloadableConfig.RELOAD_ADDRESS, Boolean.TRUE));

		MQ.init(() -> {
			try {
				mqConnected();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		});
		logger.info("BlueMind Application started");

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					ApplicationLauncher.this.stop();
				} catch (Exception ex) {
					logger.error("Error during shutdown.", ex);
				}
			}
		});

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		logger.info("Stopping BlueMind Core...");
		notifyCoreStatus("core.stopped");
		VertxPlatform.undeployVerticles(ar -> {
		});
		logger.info("BlueMind Core stopped.");
	}

	private void mqConnected() {
		MQ.registerProducer(Topic.CORE_NOTIFICATIONS);
		StateContext.start();
		DataSource ds = JdbcActivator.getInstance().getDataSource();
		logger.info("Loaded datasource: {}", ds);

		loadMailboxDataSource();

		Handler<AsyncResult<Void>> done = (AsyncResult<Void> event) -> {
			EventBus eb = VertxPlatform.eventBus();
			VertxPlatform.getVertx().setPeriodic(4000,
					tid -> eb.publish("mailbox.ds.known", JdbcActivator.getInstance().getMailboxDataSource().size()));
			eb.consumer("mailbox.ds.lookup", message -> {
				loadMailboxDataSource();
				message.reply("ok");
			});
			logger.info("Verticles deployement complete for {}, starting product checks...", BMVersion.getVersion());
			ProductChecks.asyncValidate().whenComplete((v, ex) -> {
				Startup.notifyReady();
				MemlockSupport.mlockallOrWarn();
				notifyCoreStatus("core.started");
				TimeRangeAnnotation.annotate("CORE Start", new Date(), Optional.empty(),
						ImmutableMap.of("kind", "start", "product", "bm-core"));
				if (new File(System.getProperty("user.home") + "/core.debug").exists()
						&& BMVersion.getVersion().endsWith("qualifier")) {
					StateContext.setState("core.upgrade.start");
					StateContext.setState("core.upgrade.end");
				}
				registerBluemindNode();
			});
		};

		VertxPlatform.spawnVerticles(done);
	}

	private void registerBluemindNode() {
		ApplicationInfo.register(InstallationId.getIdentifier());
	}

	private void loadMailboxDataSource() {

		File f = new File("/etc/bm/mcast.id");
		if (!f.exists()) {
			return;
		}

		Map<String, DataSource> mailboxDataSource = new HashMap<>();
		try {
			IServer serverService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IServer.class, InstallationId.getIdentifier());
			List<ItemValue<Server>> servers = serverService.allComplete();

			BmConfIni oci = new BmConfIni();
			String dbType = oci.get("dbtype");
			String login = oci.get("user");
			String password = oci.get("password");

			for (ItemValue<Server> s : servers) {
				boolean bjdata = s.value.tags.contains(TagDescriptor.bm_pgsql_data.getTag());
				if (bjdata) {
					Pool pool = BMPoolActivator.getDefault().startPool(dbType, login, password, s.value.ip, "bj-data");
					mailboxDataSource.put(s.uid, pool.getDataSource());
					BMPoolActivator.getDefault().addMailboxDataSource(s.uid, pool);
				}
			}
		} catch (Exception e) {
			logger.warn("Cannot detect data shards", e);
		}

		JdbcActivator.getInstance().setMailboxDataSource(mailboxDataSource);
		if (!mailboxDataSource.isEmpty()) {
			CacheRegistry.init();
		}

		logger.info("{} mailbox datasource found, servers: {}", mailboxDataSource.size(), mailboxDataSource.keySet());
	}

	private void notifyCoreStatus(String operation) {
		StateContext.setState(operation);
	}

}