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
package net.bluemind.imap.vertx.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.vertx.ImapResponseStatus.Status;
import net.bluemind.imap.vertx.VXStoreClient;
import net.bluemind.imap.vertx.cmd.AppendResponse;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailboxQuota;

public class OverquotaAppendTests extends WithMailboxTests {

	private static final String emlString = buildEml(4);
	private static final byte[] eml = emlString.getBytes(StandardCharsets.US_ASCII);

	private static String buildEml(int kiloBytes) {
		StringBuilder baseEml = new StringBuilder(
				"From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.currentTimeMillis()
						+ "\r\n" + "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n");
		do {
			baseEml.append("yeah yeah another line.\r\n");
		} while (baseEml.length() < kiloBytes * 1024);
		return baseEml.toString();
	}

	private IMailboxes api;
	private String uid;

	@Before
	public void before() throws Exception {
		super.before();
		this.api = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, domain);
		var mbx = api.byName(localPart);
		this.uid = mbx.uid;
		assertNotNull(mbx);
		mbx.value.quota = 1;
		api.update(mbx.uid, mbx.value);
		mbx = api.byName(localPart);
		assertEquals(1, (int) mbx.value.quota);
		ESearchActivator.refreshIndex("mailspool_alias_" + mbx.uid);
		MailboxQuota quota = api.getMailboxQuota(mbx.uid);
		System.err.println("quota: " + quota);

	}

	@Test
	public void testAppendToOverquota() throws Exception {
		VXStoreClient sc = client();

		sc.login().thenCompose(login -> {
			assertEquals(Status.Ok, login.status);
			FakeStream stream = new FakeStream(VertxPlatform.getVertx(), eml);
			return sc.append("INBOX", new Date(), Arrays.asList("\\Seen"), eml.length, stream);
		}).thenCompose(append -> {
			assertEquals(Status.Ok, append.status);
			ESearchActivator.refreshIndex("mailspool_alias_" + uid);
			MailboxQuota quota = api.getMailboxQuota(uid);
			System.err.println("quota after append: " + quota);
			FakeStream stream = new FakeStream(VertxPlatform.getVertx(), eml);
			return sc.append("INBOX", new Date(), Arrays.asList("\\Seen"), eml.length, stream);
		}).thenCompose(append -> {
			assertEquals(Status.No, append.status);
			assertTrue(append.result.isPresent());
			AppendResponse ar = append.result.get();
			System.out.println("reason: '" + ar.reason + "'");
			assertNotNull(ar.reason);
			assertEquals("Over quota", ar.reason);
			return sc.close();
		}).get(15, TimeUnit.SECONDS);
	}

}
