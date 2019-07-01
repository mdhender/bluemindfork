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

import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Producer;
import tigase.db.TigaseDBException;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.Presence;
import tigase.xmpp.impl.roster.RosterAbstract.PresenceType;

public class BMPresence extends Presence {

	private static final Logger logger = LoggerFactory.getLogger(BMPresence.class);

	public BMPresence() {
		logger.info("**************** bm-presence created. ******************");
	}

	@Override
	public String id() {
		return "bm-presence";
	}

	@Override
	protected void processOutInitial(Packet packet, XMPPResourceConnection session, Queue<Packet> results,
			Map<String, Object> settings, PresenceType type) throws NotAuthorizedException, TigaseDBException {
		super.processOutInitial(packet, session, results, settings, type);

		Producer prod = MQSetup.getProducer();
		if (prod != null) {
			OOPMessage msg = MQ.newMessage();
			msg.putStringProperty("operation", "im.presence.notification");
			msg.putStringProperty("user", session.getjid().getBareJID().toString());
			Element pres = session.getPresence();

			if (pres != null) {
				Element show = pres.getChild("show");
				if (show != null) {
					msg.putStringProperty("show", show.getCData());
				}

				prod.send(msg);
				logger.info("Send IM presence notification " + msg);
			}
		}

	}

}
