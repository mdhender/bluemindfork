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

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.IMAPException;

public class StoreClientCallback implements IResponseCallback {

	private Logger logger = LoggerFactory.getLogger(getClass());
	IMAPResponseParser rParser;

	private LinkedList<IMAPResponse> responses;
	private ClientSupport client;

	public StoreClientCallback() {
		this.rParser = new IMAPResponseParser();
		this.responses = new LinkedList<IMAPResponse>();
	}

	@Override
	public void connected() {
		logger.debug("connected() callback called.");
		rParser.setServerHelloReceived(false);
	}

	@Override
	public void disconnected() {
		logger.debug("disconnected() callback called.");
		client.throwError(new IMAPException("disconnected."));
	}

	@Override
	public void imapResponse(IoSession session, MinaIMAPMessage imapResponse) {
		IMAPResponse rp = null;
		try {
			rp = rParser.parse(session, imapResponse);
		} catch (RuntimeException re) {
			logger.warn("Runtime exception on: " + imapResponse);
			throw re;
		}

		responses.add(rp);

		if (rp.isClientDataExpected()) {
			ArrayList<IMAPResponse> rs = new ArrayList<IMAPResponse>(responses.size());
			rs.addAll(responses);
			responses.clear();
			client.setResponses(rs);
		}
	}

	@Override
	public void setClient(ClientSupport cs) {
		this.client = cs;
	}

	@Override
	public void exceptionCaught(IMAPException cause) throws IMAPException {
		client.throwError(cause);
	}

}
