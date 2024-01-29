/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.jna.utils.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.jna.utils.MemfdSupport;
import net.bluemind.jna.utils.OffHeapTemporaryFile;

public class MemFdTests {

	@Before
	public void before() {
		MemfdSupport.resetAutoReclaimCount();
	}

	/**
	 * lsof shows <code>java ... 4096    53846 /memfd:yeah-yeah (deleted)</code>
	 */
	@Test
	public void testWriteThenRead() {
		byte[] content = new byte[4096];
		ThreadLocalRandom.current().nextBytes(content);
		try (OffHeapTemporaryFile offHeap = MemfdSupport.newOffHeapTemporaryFile("yeah-yeah")) {
			try (var out = offHeap.openForWriting()) {
				out.write(content);
			}
			try (var in = offHeap.openForReading()) {
				byte[] reread = in.readAllBytes();
				assertNotNull(reread);
				assertArrayEquals(content, reread);
			}
			assertEquals(4096, offHeap.length());
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testAutoReclaim() throws IOException {
		byte[] content = new byte[4096];
		ThreadLocalRandom.current().nextBytes(content);
		for (int i = 0; i < 100_000; i++) {
			OffHeapTemporaryFile offHeap = MemfdSupport.newOffHeapTemporaryFile("yeah-" + i);
			try (var out = offHeap.openForWriting()) {
				out.write(content);
			}
			if (i % 5000 == 0) {
				System.gc();
				System.err.println("reclaimed: " + MemfdSupport.autoReclaimed());
			}
		}
		System.err.println("reclaimed: " + MemfdSupport.autoReclaimed());
		assertTrue(MemfdSupport.autoReclaimed() > 0);
	}

	@Test
	public void testNoReclaim() throws IOException {
		byte[] content = new byte[4096];
		ThreadLocalRandom.current().nextBytes(content);
		for (int i = 0; i < 100_000; i++) {
			try (OffHeapTemporaryFile offHeap = MemfdSupport.newOffHeapTemporaryFile("yeah-" + i)) {
				try (var out = offHeap.openForWriting()) {
					out.write(content);
				}
			}
			if (i % 5000 == 0) {
				System.gc();
				System.err.println("reclaimed: " + MemfdSupport.autoReclaimed());
			}
		}
		System.gc();
		System.err.println("reclaimed: " + MemfdSupport.autoReclaimed());
		assertEquals(0, MemfdSupport.autoReclaimed());
	}

}
