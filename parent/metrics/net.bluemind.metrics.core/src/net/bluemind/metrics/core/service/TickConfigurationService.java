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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.metrics.core.service;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.metrics.core.IInCoreTickConfiguration;
import net.bluemind.metrics.core.tick.TickConfigurators;
import net.bluemind.metrics.core.tick.TickInputConfigurator;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class TickConfigurationService implements IInCoreTickConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(TickConfigurationService.class);
	private final BmContext context;

	public TickConfigurationService(BmContext context) {
		this.context = context;
	}

	@Override
	public TaskRef reconfigure() {
		return context.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			reconfigure(monitor, context);
		}));
	}

	@Override
	public void reconfigure(IServerTaskMonitor monitor, BmContext ctx) {
		IServer serversApi = ctx.provider().instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> allServers = Topology.get().nodes();
		List<TickInputConfigurator> hooks = TickConfigurators.configurators();

		IServerTaskMonitor sub = monitor.subWork("tick", allServers.size());
		for (ItemValue<Server> server : allServers) {
			for (String tag : server.value.tags) {
				for (TickInputConfigurator h : hooks) {
					h.setMonitor(monitor.subWork(server.value.address(), 1));
					h.onServerTagged(ctx, server, tag);
					h.setMonitor(null);
				}
			}
			try {
				serversApi.submitAndWait(server.uid, "service", "telegraf", "restart");
				sub.log("Telegraf restarted on " + server.value.address());
			} catch (ServerFault sf) {
				// node is not running on localhost in unit tests...
			}
			sub.progress(1, server.value.address() + " configured");
		}
		CountDownLatch reconf = new CountDownLatch(2);
		final IServerTaskMonitor kapa = monitor.subWork("kapacitor", 1);
		VertxPlatform.eventBus().request("kapacitor.configuration", new JsonObject(),
				(AsyncResult<Message<JsonObject>> reply) -> {
					reconf.countDown();
					kapa.progress(1, "Kapacitor reconfigured");
				});
		final IServerTaskMonitor chrono = monitor.subWork("chronograf", 1);
		VertxPlatform.eventBus().request("chronograf.configuration", new JsonObject(),
				(AsyncResult<Message<JsonObject>> reply) -> {
					reconf.countDown();
					chrono.progress(1, "Chronograf reconfigured");
				});
		try {
			reconf.await(1, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			logger.error(e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
	}

}
