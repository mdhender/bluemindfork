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
package net.bluemind.imap.sieve;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SieveClientHandler extends IoHandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(SieveClientHandler.class);

	private SieveResponseParser srp = new SieveResponseParser();

	private IoFilter getSieveFilter() {
		ProtocolCodecFactory pcf = new SieveCodecFactory();
		return new ProtocolCodecFilter(pcf);
	}

	public SieveClientHandler() {
		// ok
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		session.getFilterChain().addLast("codec", getSieveFilter());
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		// ok
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		SieveMessage msg = (SieveMessage) message;
		SieveResponse response = srp.parse(msg);
		scs(session).setResponses(response);
	}

	private SieveClientSupport scs(IoSession session) {
		return (SieveClientSupport) session.getAttribute("scs");
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		scs(session).sessionClosed();
		session.removeAttribute("scs");
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);
		sessionClosed(session);
	}

}
