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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetClient;

public class VertxLmtpClient {

	private static final Logger logger = LoggerFactory.getLogger(VertxLmtpClient.class);
	private final Vertx vertx;
	private final NetClient client;
	private final String host;
	private final int port;
	private LmtpClientSession session;

	public VertxLmtpClient(Vertx vx, String host, int port) {
		this.vertx = vx;
		client = this.vertx.createNetClient().setTCPNoDelay(true);
		this.host = host;
		this.port = port;
		logger.info("LMTP client for {}:{}", host, port);
	}

	public CompletableFuture<String> connect() {
		CompletableFuture<String> ret = new CompletableFuture<>();
		client.connect(port, host, conResult -> {
			if (conResult.succeeded()) {
				session = new LmtpClientSession(vertx, conResult.result());
				session.start().thenAccept(banner -> {
					ret.complete(banner);
				});
			} else {
				ret.completeExceptionally(conResult.cause());
			}
		});
		return ret;
	}

	public CompletableFuture<Void> close() {
		Objects.requireNonNull(session, "Session is null / closed.");
		CompletableFuture<Void> ret = session.stop();
		session = null;
		return ret;
	}

	public CompletableFuture<Response> lhlo(String h) {
		Objects.requireNonNull(session, "Session is null / closed.");
		return session.writeCmd("LHLO " + h);
	}

	public CompletableFuture<Response> rcptTo(String recipient) {
		Objects.requireNonNull(session, "Session is null / closed.");
		return session.writeCmd("RCPT TO:<" + recipient + ">");
	}

	public CompletableFuture<Response[]> data(int validatedRecipients, Buffer eml) {
		Objects.requireNonNull(session, "Session is null / closed.");
		return session.writeCmd("DATA").thenCompose(acceptData -> {
			int len = acceptData.parts().size();
			String lastResp = acceptData.parts().get(len - 1);
			int code = Integer.parseInt(lastResp.substring(0, 3));
			logger.info("Got DATA intermediate code {}", code);
			if (code == 354) {
				Buffer withSep = eml.copy().appendBytes("\r\n.\r\n".getBytes());
				// FIXME escape the dots in the EML
				return session.writeRaw(validatedRecipients, withSep);
			} else {
				return CompletableFuture.completedFuture(new Response[] { acceptData });
			}
		});
	}

	public CompletableFuture<Response> mailFrom(String sender) {
		Objects.requireNonNull(session, "Session is null / closed.");
		return session.writeCmd("MAIL FROM:<" + sender + ">");
	}

	public CompletableFuture<List<Response>> batch(Request... requests) {
		List<Response> ret = new LinkedList<>();
		CompletableFuture<List<Response>> retProm = new CompletableFuture<>();
		CompletableFuture<?>[] responsePromises = new CompletableFuture[requests.length];
		for (int i = 0; i < requests.length; i++) {
			responsePromises[i] = session.writeCmd(requests[i].cmd()).thenCompose(resp -> {
				ret.add(resp);
				return CompletableFuture.completedFuture(null);
			});
		}
		CompletableFuture.allOf(responsePromises).thenAccept(v -> {
			retProm.complete(ret);
		});
		return retProm;
	}

	public CompletableFuture<List<Response>> batchOneChunk(Request... requests) {
		Buffer batched = new Buffer();
		for (int i = 0; i < requests.length; i++) {
			batched.appendString(requests[i].cmd()).appendString("\r\n");
		}
		CompletableFuture<List<Response>> retProm = session.writeRaw(requests.length, batched).thenApply(resps -> {
			return Arrays.asList(resps);
		});
		return retProm;
	}

}
