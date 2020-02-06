/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.lmtp.testhelper.client;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import net.bluemind.lmtp.testhelper.client.Response.ResponseBuilder;
import net.bluemind.lmtp.testhelper.common.WriteSupport;

public class LmtpClientSession {

	private static final Logger logger = LoggerFactory.getLogger(LmtpClientSession.class);

	private final Vertx vertx;
	private final NetSocket sock;
	private final CompletableFuture<String> bannerFuture;
	private final CompletableFuture<Void> closeFuture;
	private final RecordParser recordParser;
	private final WriteSupport writeSupport;
	private final Queue<CompletableFuture<Response>> responseListener;

	private static enum ParseState {
		ExpectBanner, WriteCmd, ExpectResp, WriteData;
	}

	private ParseState expectedContent;

	private ResponseBuilder responseBuilder;

	public LmtpClientSession(Vertx vertx, NetSocket sock) {
		this.vertx = vertx;
		this.sock = sock;
		bannerFuture = new CompletableFuture<>();
		closeFuture = new CompletableFuture<>();
		responseListener = new LinkedList<>();
		setState(ParseState.ExpectBanner);
		this.writeSupport = new WriteSupport(sock);
		this.recordParser = RecordParser.newDelimited("\r\n", buf -> {
			doDelimited(buf);
		});
		logger.debug("Created with vertx {}", this.vertx);
	}

	private void doDelimited(Buffer buf) {
		ParseState curState = getState();
		logger.debug("Got buf {}", curState);
		switch (curState) {
		case ExpectBanner:
			logger.info("****** BANNER RECEIVED ******");
			setState(ParseState.WriteCmd);
			bannerFuture.complete(buf.toString());
			break;
		case ExpectResp:
			processRespPart(buf);
			break;
		case WriteCmd:
			spuriousResponse(buf);
			break;
		case WriteData:
			spuriousResponse(buf);
			break;
		}
	}

	private void setState(ParseState state) {
		logger.info("STATE CHANGE: {} => {}", expectedContent, state);
		expectedContent = state;
	}

	private ParseState getState() {
		return expectedContent;
	}

	public CompletableFuture<Response> writeCmd(String cmd) {
		CompletableFuture<Response> listener = new CompletableFuture<>();
		responseListener.add(listener);
		this.responseBuilder = Response.builder();
		setState(ParseState.ExpectResp);
		writeSupport.writeWithCRLF(cmd).thenAccept(v -> {
			logger.info("C: {}", cmd);
		});

		return listener;
	}

	public CompletableFuture<Response[]> writeRaw(int validatedRecipients, Buffer raw) {

		@SuppressWarnings("unchecked")
		CompletableFuture<Response>[] listeners = new CompletableFuture[validatedRecipients];
		for (int i = 0; i < validatedRecipients; i++) {
			listeners[i] = new CompletableFuture<Response>();
			responseListener.add(listeners[i]);
		}
		this.responseBuilder = Response.builder();
		setState(ParseState.ExpectResp);
		int bufLen = raw.length();
		writeSupport.writeRaw(raw).thenAccept(v -> {
			logger.info("C: {}bytes", bufLen);
		});
		return CompletableFuture.allOf(listeners).thenCompose(v -> {
			logger.info("Got all {} resp(s)", validatedRecipients);
			Response[] allResps = new Response[listeners.length];
			for (int i = 0; i < allResps.length; i++) {
				allResps[i] = listeners[i].getNow(null);
			}
			return CompletableFuture.completedFuture(allResps);
		});
	}

	private void processRespPart(Buffer buf) {
		String respPart = buf.toString();
		boolean isLast = isLast(respPart);
		logger.info("<= '{}' (last: {}, state: {})", respPart, isLast, getState());
		if (isLast) {
			Response built = responseBuilder.build(respPart);
			this.responseBuilder = Response.builder();
			CompletableFuture<Response> listener = responseListener.poll();
			setState(responseListener.isEmpty() ? ParseState.WriteCmd : ParseState.ExpectResp);
			listener.complete(built);
		} else {
			responseBuilder.part(respPart);
		}
	}

	private boolean isLast(String buf) {
		if (buf.length() < 3) {
			logger.warn("response is too short: {}", buf);
			return false;
		}

		String code = buf.substring(0, 3).toString();
		try {
			Integer.parseInt(code);
		} catch (NumberFormatException e) {
			logger.warn("wrong response code {}", code);
		}

		char spc = buf.charAt(3);
		if (spc == '-') {
			// multiline response
			return false;
		} else if (spc == ' ') {
			return true;
		} else {
			logger.warn("wrongly formated response");
			return false;
		}
	}

	private void spuriousResponse(Buffer buf) {
		logger.warn("Unexpected response: S: {}", buf);
	}

	public CompletableFuture<String> start() {
		sock.handler(recordParser);
		sock.closeHandler(v -> {
			logger.info("Client socket {} closed.", sock.writeHandlerID());
			closeFuture.complete(null);
		});
		return bannerFuture;
	}

	public CompletableFuture<Void> stop() {
		sock.close();
		return closeFuture;
	}

}
