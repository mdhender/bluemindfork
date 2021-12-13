/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.system.ldap.export.verticle;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.IMQConnectHandler;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.OutOfProcessMessageHandler;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.system.ldap.export.services.DomainRootUpdateService;

public class MQManager implements IMQConnectHandler, OutOfProcessMessageHandler {
	private static final Logger logger = LoggerFactory.getLogger(MQManager.class);

	public static void init() {
		MQ.init(new MQManager());
	}

	@Override
	public void connected() {
		MQ.registerConsumer(Topic.SYSTEM_NOTIFICATIONS, this);
	}

	@Override
	public void handle(OOPMessage msg) {
		String op = msg.getStringProperty("operation");
		if (op == null) {
			logger.warn("operation attribute is not in message.");
			return;
		}

		if (op.equals("domain.updated")) {
			domainUpdated(msg.getStringProperty("domain"));
		}
	}

	private void domainUpdated(String domainUid) {
		long time = System.currentTimeMillis();

		try {
			Optional<DomainRootUpdateService> domainRootUpdateService = DomainRootUpdateService.build(domainUid);
			if (domainRootUpdateService.isPresent()) {
				logger.info("Update domain {} LDAP root", domainUid);
				domainRootUpdateService.get().sync();
			}
		} catch (Exception e) {
			logger.error("Error during domain {} LDAP root update", domainUid, e);
		}

		logger.info("Update domain {} LDAP root DONE in {} ms", domainUid, (System.currentTimeMillis() - time));
	}
}
