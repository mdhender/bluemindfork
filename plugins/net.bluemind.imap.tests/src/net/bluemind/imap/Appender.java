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

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Appender implements Runnable {

	private static final int CNT = 100;

	private StoreClient sc;
	private IMessageProducer mp;
	private AtomicInteger failed;
	private AtomicInteger ok;

	public Appender(StoreClient sc, IMessageProducer mp) {
		this.sc = sc;
		this.mp = mp;
		failed = new AtomicInteger(0);
		ok = new AtomicInteger(0);
	}

	@Override
	public void run() {
		FlagsList unflagged = new FlagsList();
		unflagged.add(Flag.BMARCHIVED);
		FlagsList del = new FlagsList();
		del.add(Flag.DELETED);
		int result = 0;
		for (int i = 0; i < CNT; i++) {
			IMAPByteSource msg = null;
			try {
				boolean select = sc.select("INBOX");
				assertTrue("inbox select failed", select);
				if (result == 0) {
					msg = mp.newMessageStream();
				} else {
					msg = sc.uidFetchMessage(result);
				}
				int newResult = sc.append("INBOX", msg.source().openStream(), unflagged);
				if (newResult < 0) {
					failed.incrementAndGet();
					break;
				} else {
					ok.incrementAndGet();
					boolean ok = sc.uidStore(Arrays.asList(result), del, true);
					assertTrue("flagging of " + result + " failed", ok);
					result = newResult;
					ok = sc.noop();
					assertTrue("noop failed", ok);
				}
			} catch (Exception e) {
				failed.incrementAndGet();
				e.printStackTrace();
				break;
			} finally {
				if (msg != null) {
					msg.close();
				}
			}
		}
		System.out.println(Thread.currentThread().getName() + " added " + ok.get() + " messages.");
		sc.expunge();
	}

	public int getFailed() {
		return failed.get();
	}
}
