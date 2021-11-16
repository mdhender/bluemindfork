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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.mail.replica.service.ReplicationEvents;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IEventBusAccessRule;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class PerOwnerMailboxEventProducer extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(PerOwnerMailboxEventProducer.class);
	private static final String ADDRESS_PREFIX = "mailreplica.";
	private static final String ADDRESS_SUFFIX = ".updated";

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new PerOwnerMailboxEventProducer();
		}

	}

	@Override
	public void start() {
		EventBus eb = vertx.eventBus();
		eb.consumer(ReplicationEvents.MBOX_UPD_ADDR, (Message<JsonObject> msg) -> vertx.executeBlocking(prom -> {
			String owner = msg.body().getString("owner");
			JsonObject ownerEvent = createMailboxEvent(msg, owner);
			eb.publish(ADDRESS_PREFIX + owner + ADDRESS_SUFFIX, ownerEvent);
		}, false));
		eb.consumer(ReplicationEvents.HIER_UPD_ADDR, (Message<JsonObject> msg) -> vertx.executeBlocking(prom -> {
			if (!msg.body().getBoolean("minor")) {
				String owner = msg.body().getString("owner");
				JsonObject ownerEvent = createHierarchyEvent(msg, owner);
				eb.publish(ADDRESS_PREFIX + owner + ADDRESS_SUFFIX, ownerEvent);
			}
		}, false));
	}

	private JsonObject createMailboxEvent(Message<JsonObject> msg, String owner) {
		JsonObject ownerEvent = new JsonObject();
		ownerEvent.put("isHierarchy", false);
		ownerEvent.put("mailbox", msg.body().getString("mailbox"));
		ownerEvent.put("container", msg.body().getString("container"));
		ownerEvent.put("version", msg.body().getLong("version"));
		ownerEvent.put("owner", owner);
		return ownerEvent;
	}

	private JsonObject createHierarchyEvent(Message<JsonObject> msg, String owner) {
		JsonObject ownerEvent = new JsonObject();
		ownerEvent.put("isHierarchy", true);
		ownerEvent.put("mailbox", msg.body().getString("uid"));
		ownerEvent.put("itemUid", msg.body().getString("itemUid"));
		ownerEvent.put("itemId", msg.body().getLong("itemId"));
		ownerEvent.put("version", msg.body().getLong("version"));
		ownerEvent.put("owner", owner);
		return ownerEvent;
	}

	public static final class RuleAccess implements IEventBusAccessRule {
		private static final Logger logger = LoggerFactory.getLogger(RuleAccess.class);

		@Override
		public boolean match(String path) {
			return path.startsWith(ADDRESS_PREFIX) && path.endsWith(ADDRESS_SUFFIX);
		}

		@Override
		public boolean authorize(BmContext context, String path) {
			String pathOwnerUid = path.substring(ADDRESS_PREFIX.length(), path.length() - ADDRESS_SUFFIX.length());
			String aclContainerUid = IMailboxAclUids.uidForMailbox(pathOwnerUid);
			try {
				return RBACManager.forContext(context).forContainer(aclContainerUid).can(Verb.Read.name());
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.NOT_FOUND) {
					logger.info("Authorization on non-existing container {} requested", aclContainerUid);
					return false;
				} else {
					throw e;
				}
			}
		}
	}

}
