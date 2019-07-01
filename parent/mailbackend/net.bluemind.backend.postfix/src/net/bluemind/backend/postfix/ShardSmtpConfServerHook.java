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
package net.bluemind.backend.postfix;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;

public class ShardSmtpConfServerHook extends DefaultServerHook {

	private static final Set<String> TAGS = new HashSet<>(Arrays.asList("mail/imap"));
	private static final Logger logger = LoggerFactory.getLogger(ShardSmtpConfServerHook.class);

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (!TAGS.contains(tag)) {
			return;
		}

		List<ItemValue<Server>> servers = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.allComplete();

		Optional<ItemValue<Server>> smtpServer = servers.stream()
				.filter(s -> s.value.tags.contains("mail/smtp") && !s.uid.equals(server.uid)).findAny();

		ItemValue<Server> dbServer = context.provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(server.uid);

		// if shard && mail/imap : write postfix conf
		// disable milter
		if (smtpServer.isPresent() && !dbServer.value.tags.contains("mail/smtp")) {

			logger.info("** initialize shard postfix conf, stop and disable milter, server {}, ip {}", dbServer.uid,
					dbServer.value.ip);

			PostfixService service = new PostfixService();
			service.initializeShard(dbServer, smtpServer.get());
		}

	}

}
