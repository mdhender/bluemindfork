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

import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.platform.Verticle;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.config.InstallationId;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.server.api.IServer;

public class CyrusServiceVerticle extends Verticle {

	@Override
	public void start() {
		EventBus eventBus = vertx.eventBus();

		eventBus.registerHandler("mailreplica.receiver.ready", message -> {

			IServer service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
					InstallationId.getIdentifier());

			service.allComplete().stream().filter(s -> s.value.tags.contains("mail/imap")).forEach(s -> {
				CyrusService cyrusService = new CyrusService(s.value.address());
				cyrusService.reload();
			});

		});
	}

}
