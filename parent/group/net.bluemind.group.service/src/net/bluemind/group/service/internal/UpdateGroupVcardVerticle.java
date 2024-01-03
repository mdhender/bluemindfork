/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.group.service.internal;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import net.bluemind.addressbook.persistence.VCardStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.IGroup;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lib.vertx.utils.ThrottleMessages;

public class UpdateGroupVcardVerticle extends AbstractVerticle {
	public static final String VCARD_UPDATE_BUS_ADDRESS = "group.update_vcard.queue";
	public static final String VCARD_NOTIFY_BUS_ADDRESS = "group.update_vcard.done";
	private static Logger logger = LoggerFactory.getLogger(UpdateGroupVcardVerticle.class);

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {
		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new UpdateGroupVcardVerticle();
		}
	}

	private static record GroupIdentifier(String domainUid, String groupUid) {
	}

	public UpdateGroupVcardVerticle() {
		Vertx vertx = VertxPlatform.getVertx();
		ThrottleMessages<JsonObject> throttler = new ThrottleMessages<>(msg -> {
			JsonObject body = msg.body();
			String domainUid = body.getString("domain_uid");
			String groupUid = body.getString("group_uid");
			return new GroupIdentifier(domainUid, groupUid);
		}, msg -> {
			JsonObject body = msg.body();
			String domainUid = body.getString("domain_uid");
			String groupUid = body.getString("group_uid");
			this.updateGroupVcard(new GroupIdentifier(domainUid, groupUid));
		}, vertx, 5000);
		vertx.eventBus().consumer(VCARD_UPDATE_BUS_ADDRESS, throttler);
	}

	public void updateGroupVcard(GroupIdentifier gi) {
		Container container;
		DataSource ds = ServerSideServiceProvider.defaultDataSource;
		try {
			ContainerStore containerStore = new ContainerStore(null, ds, SecurityContext.SYSTEM);
			container = containerStore.get(gi.domainUid());
		} catch (SQLException e) {
			logger.error("Unable to update group vcard", e);
			throw new ServerFault(e);
		}
		GroupVCardAdapter adapter = new GroupVCardAdapter(ds, SecurityContext.SYSTEM, container, gi.domainUid());
		VCardStore vcardStore = new VCardStore(ds, container);
		ItemValue<Group> grp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGroup.class, gi.domainUid()).getComplete(gi.groupUid());
		ItemValue<Domain> domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).get(gi.domainUid());
		try {
			vcardStore.update(grp.item(), adapter.asVCard(domain, grp.item().uid, grp.value));
		} catch (SQLException e) {
			logger.error("Unable to update group vcard", e);
		}
		JsonObject replyMessage = new JsonObject();
		replyMessage.put("domain_uid", gi.domainUid());
		replyMessage.put("group_uid", gi.groupUid());
		Vertx vertx = VertxPlatform.getVertx();
		vertx.eventBus().publish(VCARD_NOTIFY_BUS_ADDRESS, replyMessage);
	}

}
