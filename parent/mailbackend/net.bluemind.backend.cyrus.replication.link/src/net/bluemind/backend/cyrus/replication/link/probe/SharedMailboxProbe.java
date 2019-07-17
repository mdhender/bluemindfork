/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.backend.cyrus.replication.link.probe;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.Server;

public class SharedMailboxProbe {

	private static final Logger logger = LoggerFactory.getLogger(SharedMailboxProbe.class);
	private final ItemValue<Mailshare> share;
	private final String domainUid;
	private final ItemValue<Server> backend;

	public SharedMailboxProbe(ItemValue<Server> srv, String domainUid, ItemValue<Mailshare> sharedBox) {
		this.backend = srv;
		this.domainUid = domainUid;
		this.share = sharedBox;
	}

	public String domainUid() {
		return domainUid;
	}

	public ItemValue<Server> backend() {
		return backend;
	}

	public ItemValue<Mailshare> share() {
		return share;
	}

	public static String nameFor(Assignment domainServerPair) {
		return "probe_" + domainServerPair.serverUid + "__" + domainServerPair.domainUid;
	}

	public static String uidFor(Assignment domainServerPair) {
		return UUID.nameUUIDFromBytes(nameFor(domainServerPair).getBytes()).toString();
	}

	public static class Builder {
		IServiceProvider prov;
		Assignment backendAssignment;
		ItemValue<Server> srv;

		public Builder forBackend(Assignment withDomain) {
			this.backendAssignment = withDomain;
			return this;
		}

		public Builder server(ItemValue<Server> srv) {
			this.srv = srv;
			return this;
		}

		public Builder provider(IServiceProvider prov) {
			this.prov = prov;
			return this;
		}

		public SharedMailboxProbe build() {
			IMailshare sharedApi = prov.instance(IMailshare.class, backendAssignment.domainUid);
			Mailshare probe = new Mailshare();
			probe.dataLocation = backendAssignment.serverUid;
			probe.name = SharedMailboxProbe.nameFor(backendAssignment);
			probe.system = true;
			probe.hidden = true;
			probe.routing = Routing.internal;

			String uid = SharedMailboxProbe.uidFor(backendAssignment);
			logger.info("Shared mailbox probe will be {}@{} with uid {}", probe.name, backendAssignment.domainUid, uid);
			ItemValue<Mailshare> sharedBox = sharedApi.getComplete(uid);
			if (sharedBox != null) {
				logger.info("Probe is {}", sharedBox);
			} else {
				sharedApi.create(uid, probe);
				sharedBox = sharedApi.getComplete(uid);
				logger.info("Created {}", sharedBox);
			}
			return new SharedMailboxProbe(srv, backendAssignment.domainUid, sharedBox);
		}
	}

}
