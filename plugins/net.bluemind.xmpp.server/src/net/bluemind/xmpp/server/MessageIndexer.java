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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.elasticsearch.ESearchActivator;
import tigase.server.Packet;
import tigase.xmpp.BareJID;

public final class MessageIndexer {

	private static final Logger logger = LoggerFactory.getLogger(MessageIndexer.class);

	private final BareJID from;
	private final BareJID to;
	private final Packet packet;

	public MessageIndexer(BareJID from, BareJID to, Packet packet) {
		this.from = from;
		this.to = to;
		this.packet = packet;
	}

	public void index() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("id", UUID.randomUUID().toString());
		map.put("timecreate", System.currentTimeMillis());
		map.put("from", from.toString());
		map.put("to", to.toString());

		String[] body = { "message", "body" };
		String msg = packet.getElemCDataStaticStr(body);
		if (msg != null) {
			map.put("message", msg);
			logger.debug("Index message from {} to {}: '{}'", from.toString(), to.toString(), msg);
			ESearchActivator.index("im", "im", UUID.randomUUID().toString(), map);
			ESearchActivator.refreshIndex("im");
		}
	}
}
