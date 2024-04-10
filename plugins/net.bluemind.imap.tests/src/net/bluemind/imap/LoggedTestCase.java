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
package net.bluemind.imap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.google.common.collect.Lists;

import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class LoggedTestCase {

	private static final int PORT = 1143;
	protected String domainUid;
	protected String loginUid;

	@BeforeAll
	public static void beforeClass() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@AfterAll
	public static void afterClass() {
		System.clearProperty("node.local.ipaddr");
		System.clearProperty("imap.local.ipaddr");
		System.clearProperty("ahcnode.fail.https.ok");
	}

	@BeforeEach
	public void setUp() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		esServer.tags = Lists.newArrayList(TagDescriptor.bm_es.getTag());

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Lists.newArrayList(TagDescriptor.mail_imap.getTag());

		PopulateHelper.initGlobalVirt(pipo, esServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);
		ElasticsearchTestHelper.getInstance().beforeTest();

		domainUid = "test.devenv";
		loginUid = "user" + System.currentTimeMillis();
		PopulateHelper.addDomain(domainUid);
		PopulateHelper.addUser(loginUid, domainUid);
	}

	@AfterEach
	public void tearDown() throws Exception {
		System.err.println("===== AFTER starts =====");
		JdbcTestHelper.getInstance().afterTest();
		ElasticsearchTestHelper.getInstance().afterTest();
		System.err.println("===== AFTER ends =====");
	}

	protected StoreClient newStore(boolean tls) {
		StoreClient store = new StoreClient("127.0.0.1", PORT, loginUid + "@" + domainUid, loginUid);
		boolean login = store.login(tls);
		assertTrue(login);
		if (!login) {
			fail("login failed for " + login + " / " + loginUid);
		}
		return store;
	}

	protected StoreClient newStore(boolean tls, int timeoutsecs) {
		StoreClient store = new StoreClient("127.0.0.1", PORT, loginUid + "@" + domainUid, loginUid,
				(int) TimeUnit.SECONDS.toSeconds(timeoutsecs));
		boolean login = store.login(tls);
		assertTrue(login);
		if (!login) {
			fail("login failed for " + login + " / " + loginUid);
		}
		return store;
	}

	public InputStream getRfc822Message() {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.nanoTime() + "\r\n"
				+ "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my 300euros from the casino.\r\n\r\n";
		return new ByteArrayInputStream(m.getBytes());
	}

	public IMAPByteSource getUtf8Rfc822Message() {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.nanoTime() + "\r\n"
				+ "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my 300€ from the casino.\r\n\r\n";
		Random r = new Random();
		int val = r.nextInt(1000);
		StringBuilder sb = new StringBuilder(val * 50 + 500);
		sb.append(m);
		for (int i = 0; i < val; i++) {
			sb.append(i).append("\r\n");
		}
		return IMAPByteSource.wrap(sb.toString().getBytes());
	}

	public InputStream getUtf8Rfc822Message(int kiloBytes) {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.nanoTime() + "\r\n"
				+ "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my 300€ from the casino.\r\n\r\n";
		Random r = new Random();
		int val = r.nextInt(1000);
		StringBuilder sb = new StringBuilder(val * 50 + 500);
		sb.append(m);
		while (sb.length() < 1024 * kiloBytes) {
			sb.append("line and number ").append(r.nextInt(1000000)).append("\r\n");
		}
		return new ByteArrayInputStream(sb.toString().getBytes());
	}
}
