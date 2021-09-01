/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.sds;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.backend.mail.replica.api.IReplicatedDataExpiration;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class SdsMessageBodyExpirationTimer extends AbstractVerticle {
	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {
		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SdsMessageBodyExpirationTimer();
		}
	}

	ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	private static final int DEFAULT_RETENTION_DAYS = 90;
	private static final Executor executor = Executors.newSingleThreadExecutor();

	@Override
	public void start() {
		VertxPlatform.executeBlockingPeriodic(TimeUnit.DAYS.toMillis(1), this::execute);
	}

	private void execute(Long timerId) {
		executor.execute(() -> {
			int retentionDays = getRetentionDays();
			if (retentionDays >= 0) {
				Set<String> servers = getServers();
				servers.forEach(server -> {
					IReplicatedDataExpiration service = provider.instance(IReplicatedDataExpiration.class, server);
					TaskRef t = service.deleteMessageBodiesFromObjectStore(retentionDays);
					TaskUtils.wait(provider, t);
				});
			}
		});
	}

	private Set<String> getServers() {
		return Topology.get().nodes().stream().filter(iv -> iv.value.tags.contains("bm/pgsql-data")).map(iv -> iv.uid)
				.collect(Collectors.toSet());
	}

	private int getRetentionDays() {
		ISystemConfiguration confService = provider.instance(ISystemConfiguration.class);
		Integer days = confService.getValues().integerValue(SysConfKeys.sds_backup_rentention_days.name());
		return days == null ? DEFAULT_RETENTION_DAYS : days;
	}
}
