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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Random;

public abstract class LoggedTestCase extends IMAPTestCase {

	protected StoreClient sc;

	public void setUp() {
		sc = newStore(false);
	}

	protected StoreClient newStore(boolean tls) {
		StoreClient store = new StoreClient(cyrusIp, 1143, testLogin, testPass);
		boolean login = store.login(tls);
		if (!login) {
			fail("login failed for " + login + " / " + testPass);
		}
		return store;
	}

	public void tearDown() {
		sc.logout();
	}

	public InputStream getRfc822Message() {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.currentTimeMillis()
				+ "\r\n" + "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
				+ "Hi, this is message about my 300euros from the casino.\r\n\r\n";
		return new ByteArrayInputStream(m.getBytes());
	}

	public IMAPByteSource getUtf8Rfc822Message() {
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.currentTimeMillis()
				+ "\r\n" + "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
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
		String m = "From: Thomas Cataldo <thomas@zz.com>\r\n" + "Subject: test message " + System.currentTimeMillis()
				+ "\r\n" + "MIME-Version: 1.0\r\n" + "Content-Type: text/plain; CHARSET=UTF-8\r\n\r\n"
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
