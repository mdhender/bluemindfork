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

import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.MessageAll;

public class BMMessageAll extends MessageAll {

	@Override
	public String id() {
		return "bm-message-all";
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws XMPPException {

		if (session == null) {
			return;
		}

		try {
			BareJID to = (packet.getStanzaTo() != null) ? packet.getStanzaTo().getBareJID() : null;

			BareJID from = (packet.getStanzaFrom() != null) ? packet.getStanzaFrom().getBareJID() : null;

			// TO
			if (session.isUserId(to)) {
				for (XMPPResourceConnection conn : session.getActiveSessions()) {
					if (conn.isResourceSet()) {
						Packet result = packet.copyElementOnly();
						result.setPacketTo(conn.getConnectionId());
						results.offer(result);

					}
				}
				return;
			}

			// FROM
			if (session.isUserId(from)) {
				for (XMPPResourceConnection conn : session.getActiveSessions()) {
					if (!conn.getUserName().equals(session.getUserName())) {
						Packet result = packet.copyElementOnly();
						result.setPacketTo(conn.getConnectionId());
						results.offer(result);
					}
				}
				results.offer(packet.copyElementOnly());
				return;
			}
		} catch (NotAuthorizedException e) {
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					Authorization.NOT_AUTHORIZED.getCondition(), true));
		}
	}
}
