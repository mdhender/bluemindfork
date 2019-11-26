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
package net.bluemind.document.persistence.fs.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.document.persistence.fs.FilesystemStore;

public class FilesystemStoreTests {

	private FilesystemStore store;

	@Before
	public void before() {
		store = new FilesystemStore();
	}

	@Test
	public void testStore() throws ServerFault, IOException {
		InputStream is = FilesystemStoreTests.class.getClassLoader().getResourceAsStream("data/wat.jpg");
		String uid = UUID.randomUUID().toString();

		byte[] content = ByteStreams.toByteArray(is);
		store.store(uid, content);
	}

	@Test
	public void testGet() throws Exception {

		byte[] random = store.get("random");
		assertNull(random);

		try {
			InputStream is = FilesystemStoreTests.class.getClassLoader().getResourceAsStream("data/wat.jpg");
			String uid = UUID.randomUUID().toString();
			byte[] expected = ByteStreams.toByteArray(is);
			store.store(uid, expected);

			byte[] fetched = store.get(uid);

			assertTrue(Arrays.equals(expected, fetched));

		} catch (ServerFault e) {
			fail();
		}

	}

	@Test
	public void testUpdate() throws ServerFault, IOException {
		InputStream is = FilesystemStoreTests.class.getClassLoader().getResourceAsStream("data/wat.jpg");
		String uid = UUID.randomUUID().toString();
		byte[] expected = ByteStreams.toByteArray(is);
		store.store(uid, expected);

		byte[] fetched = store.get(uid);
		assertTrue(Arrays.equals(expected, fetched));

		// Update
		is = FilesystemStoreTests.class.getClassLoader().getResourceAsStream("data/panda.jpg");
		expected = ByteStreams.toByteArray(is);
		store.store(uid, expected);

		is = FilesystemStoreTests.class.getClassLoader().getResourceAsStream("data/panda.jpg");
		expected = ByteStreams.toByteArray(is);
		is.close();

		fetched = store.get(uid);
		assertTrue(Arrays.equals(expected, fetched));
	}

	@Test
	public void testDelete() throws IOException {
		try {
			store.delete("random");
		} catch (ServerFault e) {
			fail();
		}

		String uid = UUID.randomUUID().toString();

		try {
			InputStream is = FilesystemStoreTests.class.getClassLoader().getResourceAsStream("data/wat.jpg");
			byte[] expected = ByteStreams.toByteArray(is);
			store.store(uid, expected);

			byte[] fetched = store.get(uid);
			assertNotNull(fetched);
			store.delete(uid);

		} catch (ServerFault e) {
			fail();
		}

		try {
			store.delete(uid);
		} catch (ServerFault e) {
			fail();
		}

	}
}
