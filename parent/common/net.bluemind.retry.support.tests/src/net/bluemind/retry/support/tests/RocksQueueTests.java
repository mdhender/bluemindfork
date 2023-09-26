/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.retry.support.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import net.bluemind.retry.support.rocks.RocksQueue;
import net.bluemind.retry.support.rocks.RocksQueue.TailRecord;

public class RocksQueueTests {

	@Test
	public void testQueueBehaviour() {
		RocksQueue q = new RocksQueue("junit-" + System.nanoTime());
		var w = q.writer();
		w.write("yeah 0");
		w.write("yeah 1");
		w.write("yeah 2");

		var named = q.reader("named");
		TailRecord rec = named.next();
		// no commit
		TailRecord reRead = named.next();
		System.err.println("r: " + rec + " re: " + reRead);
		assertEquals(rec.payload(), reRead.payload());

		named.commit();
		var fresh = named.next();
		assertNotEquals(rec.payload(), fresh.payload());
		named.commit();
		System.err.println("fresh " + fresh.payload());

		q.compact("named");
		var another = named.next();
		System.err.println("ano: " + another.payload());
	}

	@Test
	public void testQueuePerf() throws InterruptedException {

		RocksQueue q = new RocksQueue("junit-" + System.nanoTime());
		var w = q.writer();
		var cnt = 50_000;
		var writer = CompletableFuture.runAsync(() -> {
			for (int i = 0; i < cnt; i++) {
				w.write("{yeah: %d}".formatted(i));
			}
		});

		var r = q.reader("read1");
		AtomicInteger processed = new AtomicInteger();
		var reader = CompletableFuture.runAsync(() -> {
			while (true) {
				TailRecord rec = r.next();
				if (rec != null) {
					int proc = processed.incrementAndGet();
					r.commit();
					if (proc >= cnt) {
						break;
					}
				} else {
					Thread.yield();
				}
			}
		});
		writer.join();
		reader.join();
		q.compact("read1");
		System.err.println("processed: " + processed.get());
		assertEquals(cnt, processed.get());
	}

}
