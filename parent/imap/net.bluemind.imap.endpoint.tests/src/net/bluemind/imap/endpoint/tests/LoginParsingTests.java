/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.endpoint.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.cert.Certificate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.cert.X509Certificate;

import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import net.bluemind.imap.endpoint.ImapContext;
import net.bluemind.imap.endpoint.ImapMetricsHolder;
import net.bluemind.imap.endpoint.cmd.AnalyzedCommand;
import net.bluemind.imap.endpoint.cmd.LoginCommand;
import net.bluemind.imap.endpoint.cmd.RawCommandAnalyzer;
import net.bluemind.imap.endpoint.cmd.RawImapCommand;
import net.bluemind.imap.endpoint.parsing.ImapPartSplitter;
import net.bluemind.imap.endpoint.parsing.ImapRequestParser;
import net.bluemind.lib.vertx.VertxPlatform;

public class LoginParsingTests {

	@Test
	public void parseLoginPasswords() throws IOException {
		checkParsing("11 LOGIN test.dev@devenv.blue AZERTY23", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 LOGIN \"test.dev@devenv.blue\" AZERTY23", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 LOGIN \"test.dev@devenv.blue\" \"AZERTY23\"", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 LOGIN test.dev@devenv.blue \"AZERTY23\"", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 login test.dev@devenv.blue AZERTY23", "test.dev@devenv.blue", "AZERTY23");
		checkParsing("11 LOGIN test.dev@devenv.blue azerty23", "test.dev@devenv.blue", "azerty23");
		checkParsing("11 LOGIN test.dev@devenv.blue azer\"ty23", "test.dev@devenv.blue", "azer\"ty23");
		checkParsing("11 LOGIN test.dev@devenv.blue \"azer\"ty23\"", "test.dev@devenv.blue", "azer\"ty23");
	}

	private void checkParsing(String command, String login, String password) {
		ImapContext ctx = new ImapContext(VertxPlatform.getVertx(), new FakeNetSocket(), null);
		CompletableFuture<RawImapCommand> cmdProm = new CompletableFuture<>();

		Handler<RawImapCommand> rawHand = raw -> {
			cmdProm.complete(raw);
		};

		ImapRequestParser imapRequestParser = new ImapRequestParser(rawHand);
		ImapPartSplitter split = new ImapPartSplitter(ctx, imapRequestParser, ImapMetricsHolder.get());

		Buffer toParse = Buffer.buffer(command + "\r\n");
		split.handle(toParse);

		RawImapCommand raw = cmdProm.join();

		AnalyzedCommand parsed = new RawCommandAnalyzer().analyze(raw);

		assertNotNull(parsed);
		assertTrue(parsed instanceof LoginCommand);
		LoginCommand res = (LoginCommand) parsed;
		assertEquals(login, res.login());
		assertEquals(password, res.password());
		System.err.println("login: " + res.login() + " 'password: '" + res.password() + "'");

	}

	private class FakeNetSocket implements NetSocket {

		private final String id;

		public FakeNetSocket() {
			this.id = UUID.randomUUID().toString();
		}

		@Override
		public Future<Void> write(Buffer data) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean writeQueueFull() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public NetSocket exceptionHandler(Handler<Throwable> handler) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket handler(Handler<Buffer> handler) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket pause() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket resume() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket fetch(long amount) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket endHandler(Handler<Void> endHandler) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket setWriteQueueMaxSize(int maxSize) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket drainHandler(Handler<Void> handler) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String writeHandlerID() {
			return id;
		}

		@Override
		public void write(String str, Handler<AsyncResult<Void>> handler) {
			// TODO Auto-generated method stub

		}

		@Override
		public Future<Void> write(String str) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void write(String str, String enc, Handler<AsyncResult<Void>> handler) {
			// TODO Auto-generated method stub

		}

		@Override
		public Future<Void> write(String str, String enc) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void write(Buffer message, Handler<AsyncResult<Void>> handler) {
			// TODO Auto-generated method stub

		}

		@Override
		public Future<Void> sendFile(String filename, long offset, long length) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket sendFile(String filename, long offset, long length, Handler<AsyncResult<Void>> resultHandler) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SocketAddress remoteAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SocketAddress remoteAddress(boolean real) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SocketAddress localAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SocketAddress localAddress(boolean real) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Future<Void> end() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void end(Handler<AsyncResult<Void>> handler) {
			// TODO Auto-generated method stub

		}

		@Override
		public Future<Void> close() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void close(Handler<AsyncResult<Void>> handler) {
			// TODO Auto-generated method stub

		}

		@Override
		public NetSocket closeHandler(Handler<Void> handler) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket upgradeToSsl(Handler<AsyncResult<Void>> handler) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Future<Void> upgradeToSsl() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public NetSocket upgradeToSsl(String serverName, Handler<AsyncResult<Void>> handler) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Future<Void> upgradeToSsl(String serverName) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isSsl() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public SSLSession sslSession() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public X509Certificate[] peerCertificateChain() throws SSLPeerUnverifiedException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<Certificate> peerCertificates() throws SSLPeerUnverifiedException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String indicatedServerName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String applicationLayerProtocol() {
			// TODO Auto-generated method stub
			return null;
		}

	}

}
