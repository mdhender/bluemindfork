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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import net.bluemind.lib.vertx.VertxPlatform;

public class SyncClient {

	private final String host;
	private final int port;
	private final NetClient client;
	private NetSocket server;
	private CompletableFuture<UnparsedResponse> onResponse;
	private RecordParser parser;
	private String[] expected;
	private List<String> dataFrames;
	private static final Logger logger = LoggerFactory.getLogger(SyncClient.class);

	private static final String END_OF_COMMAND = ")\r\n";

	public SyncClient(String host, int port) {
		this(VertxPlatform.getVertx(), host, port);
	}

	public SyncClient(Vertx vertx, String host, int port) {
		this.host = host;
		this.port = port;
		this.client = vertx.createNetClient(new NetClientOptions().setSsl(false).setTcpNoDelay(true));
	}

	private void setupSocket(NetSocket server) {
		this.server = server;
		this.parser = RecordParser.newDelimited("\r\n", b -> {
			logger.info("S: {}", b);
			if (onResponse != null) {
				String str = b.toString();
				boolean goodOne = false;
				for (String s : expected) {
					if (str.startsWith(s)) {
						logger.info("Matched expectation '{}'", s);
						goodOne = true;
						break;
					}
				}
				if (goodOne) {
					expected = null;
					CompletableFuture<UnparsedResponse> copy = onResponse;
					UnparsedResponse resp = new UnparsedResponse(str, dataFrames);
					onResponse = null;
					dataFrames = null;
					onResponse = null;
					copy.complete(resp);
				} else {
					dataFrames.add(str);
				}
			}
		});
		server.handler(b -> parser.handle(b));
	}

	private CompletableFuture<UnparsedResponse> onResponse(String... expected) {
		logger.debug("Waiting for next response");
		this.expected = expected;
		this.dataFrames = new LinkedList<String>();
		this.onResponse = new CompletableFuture<UnparsedResponse>();
		return onResponse;
	}

	public CompletableFuture<Void> connect() {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		client.connect(port, host, result -> {
			if (result.succeeded()) {
				setupSocket(result.result());
				onResponse("* OK ").thenAccept(b -> {
					logger.info("Got BANNER response: {}", b);
					ret.complete(null);
				});
			} else {
				ret.completeExceptionally(result.cause());
			}
		});
		return ret;
	}

	public CompletableFuture<Void> startTLS() {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		server.write("STARTTLS\r\n");
		onResponse("OK ", "NO ").thenAccept(okTls -> {
			logger.info("TLS RESP: {}", okTls);
			server.upgradeToSsl(v -> {
				logger.info("TLS negociated.");
				onResponse("* OK").thenAccept(secBanner -> {
					logger.info("POST TLS banner received: '{}'", secBanner);
					ret.complete(null);
				});
			});
		});
		return ret;
	}

	public CompletableFuture<Void> authenticate(String login, String password) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		String auth = "AUTHENTICATE PLAIN ";
		String encodedStr = authToken(login, password);
		auth += encodedStr + "\r\n";
		server.write(auth);
		onResponse("OK", "NO", "BAD").thenAccept(resp -> {
			ret.complete(null);
		});
		return ret;
	}

	public static String authToken(String login, String password) {
		ByteBuf buf = Unpooled.buffer();
		buf.writeByte(0).writeBytes(login.getBytes()).writeByte(0).writeBytes(password.getBytes());
		return Base64.encode(buf).toString(StandardCharsets.US_ASCII);
	}

	public CompletableFuture<UnparsedResponse> getUser(String loginAtDomain) {
		String getUser = String.format("GET USER %s\r\n", loginAtDomain);
		server.write(Buffer.buffer(getUser));
		return onResponse("OK", "NO", "BAD");
	}

	public CompletableFuture<UnparsedResponse> applyMessages(ReadStream<Buffer> stream) {
		String start = "APPLY MESSAGE (";
		server.write(start);
		Pump pump = Pump.pump(stream, server);
		stream.endHandler(v -> {
			server.write(END_OF_COMMAND);
		});
		pump.start();

		return onResponse("OK", "NO", "BAD");
	}

	public CompletableFuture<UnparsedResponse> applyMessage(String partition, String guid, InputStream data) {
		byte[] bytes = null;
		try {
			bytes = ByteStreams.toByteArray(data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		String apply = "APPLY MESSAGE (%{" + partition + " " + guid + " " + bytes.length + "}\r\n";
		Buffer forServer = Buffer.buffer(apply);
		forServer.appendBytes(bytes);
		forServer.appendString(END_OF_COMMAND);
		server.write(forServer);
		return onResponse("OK", "NO", "BAD");
	}

	public CompletableFuture<UnparsedResponse> rawCommand(String cmd) {
		String withCrLf = cmd + "\r\n";
		Buffer forServer = Buffer.buffer(withCrLf);
		server.write(forServer);
		return onResponse("OK", "NO", "BAD");
	}

	public CompletableFuture<UnparsedResponse> applyAnnotation(String mbox, String entry, String userId, String value) {
		// String apply = "APPLY ANNOTATION (%{" + partition + " " + guid + " " +
		// bytes.length + "}\r\n";
		String apply = "APPLY ANNOTATION %(MBOXNAME " + mboxToken(mbox) + " ENTRY " + entry + " USERID " + userId
				+ " VALUE " + value + END_OF_COMMAND;
		Buffer forServer = Buffer.buffer(apply);
		server.write(forServer);
		return onResponse("OK", "NO", "BAD");
	}

	private String mboxToken(String mbox) {
		return "{" + mbox.length() + "+}\r\n" + mbox;
	}

	public CompletableFuture<UnparsedResponse> getMeta(String loginAtDomain) {
		String getUser = String.format("GET META %s\r\n", loginAtDomain);
		server.write(Buffer.buffer(getUser));
		return onResponse("OK", "NO", "BAD");
	}

	public CompletableFuture<UnparsedResponse> getMailboxes(String... mboxes) {
		String quotedBoxes = Arrays.stream(mboxes).map(s -> "\"" + s + "\"").collect(Collectors.joining(" "));
		String getUser = String.format("GET MAILBOXES (%s)\r\n", quotedBoxes);
		server.write(Buffer.buffer(getUser));
		return onResponse("OK", "NO", "BAD");
	}

	public CompletableFuture<UnparsedResponse> getFullMailbox(String mbox) {
		String getUser = String.format("GET FULLMAILBOX \"%s\"\r\n", mbox);
		server.write(Buffer.buffer(getUser));
		return onResponse("OK", "NO", "BAD");
	}

	/**
	 * 
	 * GET FETCH %(MBOXNAME fws.fr!user.dani PARTITION bm-master__fws_fr UNIQUEID
	 * k3j4ly1rzg13gtex5nwmo3r5 GUID 3935e077b8a883b05105e1984166542c3ab2cdab UID
	 * 13238)
	 * 
	 * 
	 * 
	 * @param partition
	 * @param mbox
	 * @param uniqueId
	 * @param guid
	 * @param imapUid
	 * @return
	 */
	public CompletableFuture<UnparsedResponse> fetch(String partition, String mbox, String uniqueId, String bodyGuid,
			long imapUid) {
		String getUser = String.format("GET FETCH %c(MBOXNAME %s PARTITION %s UNIQUEID %s GUID %s UID %s)\r\n", '%',
				mbox, partition, uniqueId, bodyGuid, Long.toString(imapUid));
		server.write(Buffer.buffer(getUser));
		return onResponse("OK", "NO", "BAD");
	}

	public CompletableFuture<Void> disconnect() {
		if (server != null) {
			server.close();
		} else {
			logger.warn("Not connected to {}:{}", host, port);
		}
		return CompletableFuture.completedFuture(null);
	}

	public CompletionStage<UnparsedResponse> applyReserve(String partition, List<String> mailboxes,
			List<String> guids) {
		String applyReserve = "APPLY RESERVE %(PARTITION " + partition + " MBOXNAME (" + String.join(" ", mailboxes)
				+ ") GUID (" + String.join(" ", guids) + ")" + END_OF_COMMAND;
		logger.info("C: '" + applyReserve + "'");
		server.write(Buffer.buffer(applyReserve));
		return onResponse("OK", "NO", "BAD");
	}
}
