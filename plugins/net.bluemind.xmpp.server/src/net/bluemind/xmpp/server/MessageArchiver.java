/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.xmpp.server;

import java.util.Map;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Registry;

import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.xmpp.BareJID;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;

public class MessageArchiver extends XMPPProcessor implements XMPPPreprocessorIfc {

	private static final Logger logger = LoggerFactory.getLogger(MessageArchiver.class);
	private static final Registry registry = MetricsRegistry.get();
	private final IdFactory idFactory;

	public MessageArchiver() {
		logger.info("**************** bm-xmpp-archive created. ******************");
		idFactory = new IdFactory(registry, this.getClass());
	}

	@Override
	public String id() {
		return "bm-xmpp-archive";
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) {
		registry.counter(idFactory.name("packetsCount", "type", "all")).increment();

		if (session == null) {
			// if the value is ‘true’ then the packet is blocked and no further
			// processing is performed
			return false;
		}

		StanzaType type = packet.getType();
		if (type == StanzaType.chat || type == StanzaType.groupchat) {
			registry.counter(idFactory.name("packetsCount", "type", "chat")).increment();
			String pFrom = packet.getFrom().getLocalpart();
			// only index things coming from client, not our internal routing
			if ("bosh".equals(pFrom) || "c2s".equals(pFrom)) {
				BareJID from = packet.getStanzaFrom() != null ? packet.getStanzaFrom().getBareJID() : null;
				BareJID to = packet.getStanzaTo() != null ? packet.getStanzaTo().getBareJID() : null;

				if (to != null && from != null) {
					MessageIndexer mi = new MessageIndexer(from, to, packet);
					mi.index();
				}
			}
		}
		// if the value is ‘true’ then the packet is blocked and no further
		// processing is performed
		return false;
	}
}
