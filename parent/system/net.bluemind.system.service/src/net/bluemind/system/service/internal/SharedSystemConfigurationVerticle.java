/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.system.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;

public class SharedSystemConfigurationVerticle extends AbstractVerticle {
	public static class Init implements IVerticleFactory {
		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SharedSystemConfigurationVerticle();
		}
	}

	public static class Updater implements ISystemConfigurationObserver {
		@Override
		public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
			MQ.init().thenAccept(v -> {
				SharedMap<String, String> clusterConf = MQ.sharedMap(Shared.MAP_SYSCONF);
				conf.values.forEach(clusterConf::put);
				for (String k : previous.values.keySet()) {
					if (!conf.values.containsKey(k)) {
						clusterConf.remove(k);
					}
				}
			}).exceptionally(t -> {
				logger.error(t.getMessage(), t);
				return null;
			});
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(SharedSystemConfigurationVerticle.class);

	@Override
	public void start() {
		MQ.init().thenAccept(v -> {
			IServiceProvider sysprov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			ISystemConfiguration sysconfApi = sysprov.instance(ISystemConfiguration.class);
			SystemConf values = sysconfApi.getValues();
			SharedMap<String, String> clusterConf = MQ.sharedMap(Shared.MAP_SYSCONF);
			clusterConf.putAll(values.values);
			logger.info("Sysconf pre-loaded with {} values", values.values.size());
		}).exceptionally(t -> {
			logger.warn(t.getMessage());
			return null;
		});
	}
}
