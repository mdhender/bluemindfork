/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.backend.cyrus.replication.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

public class SyncClientOIO implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(SyncClientOIO.class);
	private Socket sock;
	private Thread readLoop;
	private final LinkedBlockingDeque<String> linesQueue;
	private OutputStream output;
	private volatile boolean stopped;

	public SyncClientOIO(String host, int port) throws IOException {
		this.sock = new Socket();
		sock.setTcpNoDelay(true);
		sock.connect(new InetSocketAddress(host, port));
		this.linesQueue = new LinkedBlockingDeque<>();
		RecordParser rpi = RecordParser.newDelimited(Buffer.buffer("\r\n".getBytes()));
		rpi.handler(buf -> linesQueue.add(buf.toString(StandardCharsets.US_ASCII)));
		this.readLoop = new Thread(() -> {
			try {
				InputStream in = sock.getInputStream();
				byte[] chunk = new byte[4096];
				while (!stopped && !sock.isClosed()) {
					int read = in.read(chunk, 0, 4096);
					if (read == -1) {
						break;
					}
					if (read > 0) {
						Buffer b = Buffer.buffer();
						b.appendBytes(chunk, 0, read);
						rpi.handle(b);
					}
				}
			} catch (IOException e) {
				if (!stopped) {
					logger.error(e.getMessage(), e);
				}
			}
			if (sock.isClosed() && !stopped) {
				logger.warn("Socket {} closed, this was unexpected.", sock);
			}
		});
		this.output = sock.getOutputStream();

		readLoop.start();
		while (true) {
			String l;
			try {
				l = linesQueue.poll(1, TimeUnit.SECONDS);
				if (l != null && l.startsWith("* OK ")) {
					if (logger.isDebugEnabled()) {
						logger.debug("S: {}", l);
					}
					break;
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new SyncClientException(e.getMessage());
			}
		}
	}

	private static final Set<String> EXPECT = Sets.newHashSet("OK", "NO", "BAD");

	public String run(String cmd, String... expec) throws IOException {
		return run(cmd.getBytes(StandardCharsets.US_ASCII), null, s -> {
		}, Sets.newHashSet(expec));
	}

	public String run(String cmd) throws IOException {
		return run(cmd.getBytes(StandardCharsets.US_ASCII), null, s -> {
		}, EXPECT);
	}

	private String run(byte[] cmd, String forLog, Consumer<String> unexpected, Set<String> expectations)
			throws IOException {

		if (logger.isDebugEnabled()) {
			logger.debug("C: {}", (forLog != null ? forLog : new String(cmd)));
		}

		output.write(cmd);
		int attempts = 0;
		while (true) {
			try {
				String l = linesQueue.poll(1, TimeUnit.SECONDS);
				if (l != null) {
					int idx = l.indexOf(' ');
					if (idx > 0) {
						String first = l.substring(0, idx);
						if (expectations.contains(first)) {
							logger.debug("S: Matched with '{}': {}", first, l);
							return l;
						} else {
							logger.debug("S: {}", l);
							unexpected.accept(l);
						}
					}
				} else {
					attempts++;
					if (sock.isClosed()) {
						logger.error("Abort command on closed socket {}", sock);
						break;
					} else if (attempts >= 10) {
						logger.error("Abort after {} attempts.", attempts);
						throw new SyncClientException("to many attempts for " + new String(cmd));
					} else {
						logger.warn("waiting for {} outcome...", (forLog != null ? forLog : new String(cmd)));
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return null;
	}

	public static String authToken(String login, String password) {
		ByteBuf buf = Unpooled.buffer();
		buf.writeByte(0).writeBytes(login.getBytes()).writeByte(0).writeBytes(password.getBytes());
		return Base64.encode(buf).toString(StandardCharsets.US_ASCII);
	}

	public void authenticate(String login, String pass) throws IOException {
		run("AUTHENTICATE PLAIN " + authToken(login, pass) + "\r\n");
	}

	@Override
	public void close() throws IOException {
		stopped = true;
		this.sock.close();
	}

	public String applySub(UserMailboxSub sub) throws IOException {
		return run(sub.applySubCommand());
	}

	public String applyMailbox(ReplMailbox mbox) throws IOException {
		return run(mbox.applyMailboxCommand());
	}

	public String applyMailbox(ReplMailbox mbox, String message) throws IOException {
		return run(mbox.applyMailboxCommand(message));
	}

	public GetMailboxResponse getMailbox(String cyrusBox) throws IOException {
		GetMailboxResponse resp = new GetMailboxResponse();
		resp.statusResponse = run(("GET MAILBOXES (\"" + cyrusBox + "\")\r\n").getBytes(StandardCharsets.US_ASCII),
				"get " + cyrusBox, resp.spurious::add, EXPECT);
		return resp;
	}

	public String applyMessage(String name, String body, byte[] emlData) throws IOException {
		Buffer buf = Buffer.buffer();
		buf.appendString("APPLY MESSAGE (%{");
		buf.appendString(name).appendString(" ").appendString(body).appendString(" ")
				.appendString(Integer.toString(emlData.length)).appendString("}\r\n");
		buf.appendBytes(emlData);
		buf.appendString(")\r\n");
		return run(buf.getBytes(), "APPLY MESSAGE (%{" + name + " " + body + " " + emlData.length + "} ...)", s -> {
		}, EXPECT);
	}

}
