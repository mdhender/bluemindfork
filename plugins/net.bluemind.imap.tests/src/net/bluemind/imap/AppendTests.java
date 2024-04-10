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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.dom.Message;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.OffloadedBodyFactory;

public class AppendTests extends LoggedTestCase implements IMessageProducer {

	@Test
	public void testBug4809ThreadedAppend() throws InterruptedException {
		try (StoreClient sc1 = newStore(false);
				StoreClient sc2 = newStore(false);
				StoreClient sc3 = newStore(false);
				StoreClient sc4 = newStore(false);
				StoreClient sc5 = newStore(false)) {
			Appender ap1 = new Appender(sc1, this);
			var thread1 = CompletableFuture.runAsync(ap1);

			Appender ap2 = new Appender(sc2, this);
			var thread2 = CompletableFuture.runAsync(ap2);

			Appender ap3 = new Appender(sc3, this);
			var thread3 = CompletableFuture.runAsync(ap3);

			Appender ap4 = new Appender(sc4, this);
			var thread4 = CompletableFuture.runAsync(ap4);

			Appender ap5 = new Appender(sc5, this);
			var thread5 = CompletableFuture.runAsync(ap5);

			CompletableFuture.allOf(thread1, thread2, thread3, thread4, thread5).orTimeout(1, TimeUnit.MINUTES).join();

			assertEquals(0, ap1.getFailed());
			assertEquals(0, ap2.getFailed());
			assertEquals(0, ap3.getFailed());
			assertEquals(0, ap4.getFailed());
			assertEquals(0, ap5.getFailed());
		}
	}

	@Test
	public void testBM20597EnsureDateIsKept() throws IMAPException {
		try (StoreClient sc = newStore(false)) {
			InputStream bigInput = getUtf8Rfc822Message(32);
			FlagsList fl = new FlagsList();
			fl.add(Flag.SEEN);
			ZonedDateTime zoned = LocalDateTime.of(2024, 1, 30, 15, 37).atZone(ZoneId.of("Europe/Paris"));
			Date asDate = Date.from(zoned.toInstant());
			int result = sc.append("INBOX", bigInput, fl, asDate);
			assertTrue(result > 0);
			assertTrue(sc.noop());
			sc.select("INBOX");
			Collection<Summary> summary = sc.uidFetchSummary(Integer.toString(result));
			Summary sum = summary.iterator().next();
			System.err.println("IN  id: " + asDate);
			System.err.println("OUT id: " + sum.getDate());
			assertEquals(asDate, sum.getDate());
		}
	}

	@Test
	public void testBM12019BigAppend() throws IMAPException, Exception {
		try (StoreClient sc = newStore(false)) {
			InputStream bigInput = getUtf8Rfc822Message(19 * 1024);
			FlagsList fl = new FlagsList();
			fl.add(Flag.SEEN);
			int result = sc.append("INBOX", bigInput, fl);
			assertTrue(result > 0);
			assertTrue(sc.noop());
			sc.select("INBOX");
			IMAPByteSource fetched = sc.uidFetchMessage(result);
			assertNotNull(fetched);
			System.err.println("Fetched " + fetched.size() + " byte(s)");
			assertTrue(fetched.size() > 18 * 1024 * 1024);
			OffloadedBodyFactory offload = new OffloadedBodyFactory();
			try (Message parsed = Mime4JHelper.parse(fetched.source().openBufferedStream(), offload)) {
				assertNotNull(parsed);
				sc.uidExpunge(Arrays.asList(result));
			}
			fetched.close();
		}
	}

	@Override
	public IMAPByteSource newMessageStream() {
		return getUtf8Rfc822Message();
	}

}
