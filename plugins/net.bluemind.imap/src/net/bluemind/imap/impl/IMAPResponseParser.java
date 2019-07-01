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
package net.bluemind.imap.impl;

import java.util.List;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.IMAPByteSource;

public class IMAPResponseParser {

	private Logger logger = LoggerFactory.getLogger(getClass());
	private boolean serverHelloReceived;

	public IMAPResponseParser() {
		serverHelloReceived = false;
	}

	public IMAPResponse parse(IoSession session, MinaIMAPMessage msg) {
		String response = msg.getMessageLine();
		IMAPResponse r = new IMAPResponse();
		int idx = response.indexOf(' ');
		if (idx < 0) {
			logger.warn("response to '" + session.getAttribute("activeCommand")
					+ "'without space (forcing bad status): " + response);
			r.setStatus("BAD");
			r.setPayload(response);
			session.close(false);
			return r;
		}

		String tag = response.substring(0, idx);
		r.setTag(tag);
		int statusIdx = response.indexOf(' ', idx + 1);
		if (statusIdx < 0) {
			statusIdx = response.length();
		}
		String status = response.substring(idx + 1, statusIdx);
		if (logger.isDebugEnabled()) {
			logger.debug("TAG: " + tag + " STATUS: " + status);
		}
		r.setStatus(status);

		boolean clientDataExpected = false;
		if ("+".equals(tag) || !"*".equals(tag)) {
			clientDataExpected = true;
		}

		if (!serverHelloReceived) {
			clientDataExpected = true;
			serverHelloReceived = true;
		}
		r.setClientDataExpected(clientDataExpected);

		r.setPayload(response);

		if (msg.hasFragments()) {
			List<IMAPByteSource> all = msg.getFragments();
			int listSize = all.size();
			// why the fuck did we merge all literals ?
			if (listSize != 1) {
				throw new RuntimeException("Multiple literals in response not supported");
			}
			IMAPByteSource streamData = all.get(0);
			r.setStreamData(streamData);
		}

		return r;
	}

	public void setServerHelloReceived(boolean serverHelloReceived) {
		this.serverHelloReceived = serverHelloReceived;
	}

}
