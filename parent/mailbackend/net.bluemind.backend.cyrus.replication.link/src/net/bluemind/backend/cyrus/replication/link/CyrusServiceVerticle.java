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
package net.bluemind.backend.cyrus.replication.link;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.replication.link.probe.ReplicationLatencyTimer;
import net.bluemind.backend.cyrus.replication.link.probe.SharedMailboxProbe;
import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class CyrusServiceVerticle extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(CyrusServiceVerticle.class);

	@Override
	public void start() {
		EventBus eventBus = vertx.eventBus();

		eventBus.consumer("mailreplica.receiver.ready", message -> {
			probe(0);
		});
	}

	private void probe(long id) {
		IServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IServer service = prov.instance(IServer.class, InstallationId.getIdentifier());

		List<ItemValue<Server>> servers = service.allComplete().stream().filter(s -> s.value.tags.contains("mail/imap"))
				.collect(Collectors.toList());
		if (servers.isEmpty()) {
			vertx.setTimer(60000, this::probe);
			return;
		}

		servers.forEach(s -> {
			CyrusService cyrusService = new CyrusService(s.value.address());
			cyrusService.reload();
			vertx.setTimer(60000, timer -> buildSharedMailboxProbe(prov, service, s));
		});
	}

	private void buildSharedMailboxProbe(IServiceProvider prov, IServer service, ItemValue<Server> s) {
		List<Assignment> assignments = service.getServerAssignments(s.uid).stream()
				.filter(as -> "mail/imap".equals(as.tag)).collect(Collectors.toList());
		for (Assignment domainServerPair : assignments) {
			try {
				SharedMailboxProbe probe = new SharedMailboxProbe.Builder()//
						.forBackend(domainServerPair)//
						.server(s)//
						.provider(prov)//
						.build();
				logger.info("Probe mailbox is {}", probe);
				new ReplicationLatencyTimer(vertx, probe).start();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

}
