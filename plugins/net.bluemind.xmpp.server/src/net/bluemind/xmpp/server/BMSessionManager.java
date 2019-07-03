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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.user.api.User;
import tigase.db.TigaseDBException;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.BareJID;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;

public class BMSessionManager extends SessionManager {

	private static final Logger logger = LoggerFactory.getLogger(BMSessionManager.class);

	private static final int PRIORITY = 2;

	public BMSessionManager() {
		super();
		logger.info("*********** BM SESSION MANAGER ************");
		MQSetup.getMqListener().setSessionManager(this);
		BMUserRepo.setSessionManager(this);
	}

	/**
	 * @param msg
	 */
	public void updatePresence(OOPMessage msg) {
		String user = msg.getStringProperty("user");
		String presence = msg.getStringProperty("presence");
		String statusMsg = null;
		try {
			ItemValue<User> u = CF.user(BareJID.bareJIDInstance(user));
			XMPPSession session = getSession(BareJID.bareJIDInstance(u.value.defaultEmail().address));

			if (session == null) {
				logger.debug("No xmpp session for {}", user);
				return;
			}

			if ("calendar.dnd".equals(presence)) {
				String lang = CF.getLang(u, BareJID.bareJIDInstance(user));
				if (lang != null && "fr".equals(lang)) {
					statusMsg = "Occupé (en réunion)";
				} else {
					statusMsg = "Busy (meeting)";
				}
				presence = "dnd";
			}

			updatePresence(u.value.defaultEmail().address, presence, statusMsg);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * @param msg
	 */
	public void updatePhoneStatus(OOPMessage msg) {
		String user = msg.getStringProperty("latd");
		String status = msg.getStringProperty("status");

		try {
			ItemValue<User> u = CF.user(BareJID.bareJIDInstance(user));
			XMPPSession session = getSession(BareJID.bareJIDInstance(u.value.defaultEmail().address));

			if (session == null) {
				logger.debug("No xmpp session for {}", user);
				return;
			}

			if ("CALLING".equals(status) || "BUSY".equals(status)) {
				updatePresence(u.value.defaultEmail().address, "dnd", null);
			} else if ("AVAILABLE".equals(status)) {
				// FIXME fetch last presence
				updatePresence(u.value.defaultEmail().address, "online", null);
			}
		} catch (TigaseDBException | TigaseStringprepException e) {
			logger.error(e.getMessage(), e);
		}

	}

	/**
	 * @param latd
	 * @param presence
	 * @param status
	 */
	private void updatePresence(String jabberId, String presence, String status) {
		try {

			Element p = new Element("presence");

			if (!"online".equals(presence)) {
				p.addChild(new Element("show", presence));
			}

			if (status != null) {
				p.addChild(new Element("status", status));
			}

			p.addChild(new Element("priority", Integer.toString(PRIORITY)));

			ItemValue<User> u = CF.user(BareJID.bareJIDInstance(jabberId));
			XMPPSession sess = getSession(BareJID.bareJIDInstance(u.value.defaultEmail().address));

			if (sess != null) {
				List<XMPPResourceConnection> res = sess.getActiveResources();
				for (XMPPResourceConnection conn : res) {
					p.setAttribute("from", conn.getJID().toString());
					Packet packet = Packet.packetInstance(p);
					packet.setXMLNS("jabber:client");
					packet.setPacketTo(getComponentId());
					packet.setPacketFrom(conn.getConnectionId());

					addOutPacket(packet);
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	/**
	 * @param jabberId
	 * @return
	 * @throws TigaseStringprepException
	 */
	public boolean hasSession(String jabberId) throws TigaseStringprepException {
		XMPPSession sess = null;
		try {
			ItemValue<User> u = CF.user(BareJID.bareJIDInstance(jabberId));
			sess = getSession(BareJID.bareJIDInstance(u.value.defaultEmail().address));
		} catch (TigaseDBException e) {
			logger.error(e.getMessage(), e);
		}
		return sess != null;
	}
}
