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
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;

/**
 * Simple lmtp request parser
 *
 */
public class LmtpRequestParser implements Handler<Buffer> {

	private static Logger logger = LoggerFactory.getLogger(LmtpRequestParser.class);
	private static final byte SPACE = " ".getBytes()[0];

	public static enum State {
		Cmd, Data
	};

	private State state = State.Cmd;
	private boolean paused;
	private LmtpRequestHandler requestHandler;

	private Queue<Buffer> buffersQueue = new LinkedList<Buffer>();
	private BufferedLineParser lineParser = new BufferedLineParser();
	private String sockId;

	public LmtpRequestParser(String sockId, LmtpRequestHandler requestHandler) {
		this.requestHandler = requestHandler;
		lineParser.setDelimitedMode("\r\n");
		this.sockId = sockId;
	}

	@Override
	public void handle(Buffer buffer) {
		logger.debug("queue buffer {}", buffer);
		ProtocolLogger.logger.trace("{} REQUEST-PROTOCOL \n{}", sockId, buffer);
		buffersQueue.add(buffer);
		flush();
	}

	public void flush() {

		Buffer next = null;
		while (paused == false && (((next = lineParser.next()) != null) || !buffersQueue.isEmpty())) {
			if (next == BufferedLineParser.NEED_MORE) {
				if (buffersQueue.isEmpty()) {
					logger.debug("we need more");
					return;
				} else {
					lineParser.feed(buffersQueue.poll());
				}
			} else if (next != null) {
				switch (state) {
				case Cmd:
					logger.debug("request \n{}\n end request", next);
					parseCmd(next);
					break;
				case Data:
					logger.debug("request data size ", next.length());
					requestHandler.handleDataBuffer(next);
					break;
				}
			}
		}

	}

	private void parseCmd(Buffer buff) {
		int len = buff.length();

		if (len == 0) {
			// empty line
			return;
		}
		int end = len;
		for (int pos = 0; pos < len; pos++) {
			if (buff.getByte(pos) == SPACE) {
				end = pos;
				break;
			}
		}

		String cmd = buff.getString(0, end);

		String params = null;

		if (end < len) {
			params = buff.getString(end + 1, len);
		} else {
			params = "";
		}

		handleCmd(cmd, params);
	}

	private void handleCmd(String cmd, String params) {
		logger.debug("cmd {} params {}", cmd, params);
		// handle basic cmd

		int ch = cmd.charAt(0);

		// Breaking out of this switch causes a syntax error to be returned
		// So if you process a command then return immediately (even if the
		// command handler reported a syntax error or failed otherwise)

		switch (ch) {

		case 'l':
		case 'L':
			if ("LHLO".equalsIgnoreCase(cmd)) {
				requestHandler.handleLHLO(params);
				return;
			}
			break;

		case 'm':
		case 'M':
			if ("MAIL".equalsIgnoreCase(cmd)) {
				requestHandler.handleMAIL(params);
				return;
			}
			break;

		case 'r':
		case 'R':
			if ("RSET".equalsIgnoreCase(cmd)) {
				requestHandler.handleRSET(params);
				return;
			}
			if ("RCPT".equalsIgnoreCase(cmd)) {
				requestHandler.handleRCPT(params);
				return;
			}
			break;

		case 'n':
		case 'N':
			if ("NOOP".equalsIgnoreCase(cmd)) {
				requestHandler.handleNOOP();
				return;
			}
			break;

		case 'q':
		case 'Q':
			if ("QUIT".equalsIgnoreCase(cmd)) {
				requestHandler.handleQUIT();
				return;
			}
			break;

		case 'v':
		case 'V':
			if ("VRFY".equalsIgnoreCase(cmd)) {
				requestHandler.handleVRFY(params);
				return;
			}
			break;

		case 'd':
		case 'D':
			if ("DATA".equalsIgnoreCase(cmd)) {
				requestHandler.handleDATA(params);
				return;
			}
			break;
		default:
			break;
		}

		requestHandler.handleUnknow(cmd, params);
	}

	public void setState(State state) {
		logger.debug("change state : from {} to {}", this.state, state);
		this.state = state;
		switch (state) {
		case Data:
			lineParser.setDelimitedMode("\r\n.\r\n");
			break;
		case Cmd:
			lineParser.setDelimitedMode("\r\n");
			break;
		default:
			break;
		}
	}

	public void reset() {
		setState(State.Cmd);
	}

	public void pause() {
		this.paused = true;
	}

	public void resume() {
		this.paused = false;
		flush();
	}
}
