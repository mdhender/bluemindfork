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

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.IMAPException;

public class ClientHandler extends IoHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

	private IoFilter getIoFilter() {
		ProtocolCodecFactory pcf = new IMAPCodecFactory();
		return new ProtocolCodecFilter(pcf);
	}

	public ClientHandler() {
	}

	public void sessionCreated(IoSession session) throws Exception {
		session.getFilterChain().addLast("codec", getIoFilter());
	}

	public void sessionOpened(IoSession session) throws Exception {
		callback(session).connected();
	}

	public void messageReceived(IoSession session, Object message) throws Exception {
		MinaIMAPMessage msg = (MinaIMAPMessage) message;
		callback(session).imapResponse(session, msg);
	}

	public void sessionClosed(IoSession session) throws Exception {
		IResponseCallback cb = callback(session);
		session.removeAttribute("callback");
		cb.disconnected();
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		logger.error(cause.getMessage(), cause);
		IResponseCallback cb = callback(session);
		if (cb != null) {
			cb.exceptionCaught(new IMAPException(cause.getMessage(), cause));
		}
	}

	private final IResponseCallback callback(IoSession ios) {
		return (IResponseCallback) ios.getAttribute("callback");
	}

}
