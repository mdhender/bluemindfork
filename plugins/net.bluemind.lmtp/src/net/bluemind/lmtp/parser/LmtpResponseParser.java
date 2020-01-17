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
package net.bluemind.lmtp.parser;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.lmtp.impl.LmtpResponse;

/**
 * Simple lmtp response parser
 *
 */
public class LmtpResponseParser implements Handler<Buffer> {

	private static final Logger logger = LoggerFactory.getLogger(LmtpResponseParser.class);

	private Handler<Buffer> lineHandler = new Handler<Buffer>() {

		@Override
		public void handle(final Buffer buf) {
			handleDelimited(buf);
		}
	};

	private List<String> response = new LinkedList<>();

	private RecordParser recordParser = RecordParser.newDelimited("\r\n", lineHandler);

	private LmtpResponseHandler responseHandler;

	private String sockId;

	public LmtpResponseParser(String sockId, LmtpResponseHandler handler) {
		this.sockId = sockId;
		this.responseHandler = handler;
	}

	@Override
	public void handle(Buffer buf) {
		ProtocolLogger.logger.trace("{} RESPONSE-PROTOCOL \n{}", sockId, buf);
		recordParser.handle(buf);
	}

	private void handleDelimited(Buffer buf) {
		logger.debug("response \n{}\nend response", buf);
		if (buf.length() < 3) {
			logger.warn("response is too short");
			return;
		}

		String code = buf.getBuffer(0, 3).toString();
		try {
			Integer.parseInt(code);
		} catch (NumberFormatException e) {
			logger.warn("wrong response code {}", code);
		}

		byte spc = buf.getByte(3);
		if (spc == '-') {
			// multiline response
			response.add(buf.toString());
		} else if (spc == ' ') {
			doResponse(buf);
		} else {
			logger.warn("wrongly formated response");
		}
	}

	private void doResponse(Buffer lastMessage) {
		LmtpResponse resp = LmtpResponse.create(response, lastMessage.toString());
		logger.debug("resp {} {}", resp.getCode(), resp.getResponseMessage());

		responseHandler.handleResponse(resp);
		response = new LinkedList<>();
	}
}
