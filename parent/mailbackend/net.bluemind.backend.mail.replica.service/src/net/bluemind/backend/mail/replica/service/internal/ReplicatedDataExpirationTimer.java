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
package net.bluemind.backend.mail.replica.service.internal;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.platform.Verticle;

import net.bluemind.backend.mail.replica.api.IReplicatedDataExpiration;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.network.topology.Topology;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class ReplicatedDataExpirationTimer extends Verticle {

	ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	private static final int default_expiration = 7;
	private static final Logger logger = LoggerFactory.getLogger(ReplicatedDataExpirationTimer.class);
	private static final Executor executor = Executors.newSingleThreadExecutor();

	@Override
	public void start() {
		super.vertx.setPeriodic(TimeUnit.HOURS.toMillis(2), this::execute);
	}

	private void execute(Long timerId) {

		executor.execute(() -> {
			int expiration = getExpiration();
			logger.info("Expiring expunged messages older than {} days (tid {})", expiration, timerId);
			Set<String> servers = getServers();
			servers.forEach(server -> {
				long time = System.currentTimeMillis();
				IReplicatedDataExpiration service = provider.instance(IReplicatedDataExpiration.class, server);
				TaskUtils.wait(provider, service.deleteExpired(expiration));
				service.deleteOrphanMessageBodies();
				time = System.currentTimeMillis() - time;
				logger.info("cleanup process took {}ms.", time);

			});
		});
	}

	private Set<String> getServers() {
		Set<String> servers = Topology.get().nodes().stream().filter(iv -> iv.value.tags.contains("bm/pgsql-data"))
				.map(iv -> iv.uid).collect(Collectors.toSet());
		return servers;
	}

	private int getExpiration() {
		ISystemConfiguration confService = provider.instance(ISystemConfiguration.class);
		Integer exp = confService.getValues().integerValue(SysConfKeys.cyrus_expunged_retention_time.name());
		return exp == null ? default_expiration : exp;
	}

}
