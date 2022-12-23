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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;

import org.apache.james.mime4j.dom.Message;
import org.junit.Test;

import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.mime4j.common.OffloadedBodyFactory;

public class AppendTests extends LoggedTestCase implements IMessageProducer {

	@Test
	public void testBug4809ThreadedAppend() throws InterruptedException {
		try (StoreClient sc1 = newStore(true); StoreClient sc2 = newStore(true)) {
			Appender ap1 = new Appender(sc1, this);
			Thread t1 = new Thread(ap1);
			t1.start();

			Appender ap2 = new Appender(sc2, this);
			Thread t2 = new Thread(ap2);
			t2.start();

			t1.join();
			t2.join();

			sc1.logout();
			sc2.logout();

			assertEquals(0, ap1.getFailed());
			assertEquals(0, ap2.getFailed());
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
