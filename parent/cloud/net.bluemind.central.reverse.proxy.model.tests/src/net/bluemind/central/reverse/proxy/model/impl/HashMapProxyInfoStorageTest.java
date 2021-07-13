package net.bluemind.central.reverse.proxy.model.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class HashMapProxyInfoStorageTest {

	@Test
	public void testFull() {
		HashMapProxyInfoStorage storage = new HashMapProxyInfoStorage();
		storage.addLogin("one", "location");
		storage.addLogin("two", "location");
		storage.addLogin("three", "direction");
		storage.addDataLocation("location", "1");
		storage.addDataLocation("direction", "2");
		storage.addDataLocation("situation", "3");

		assertEquals("1", storage.ip("one"));
		assertEquals("1", storage.ip("two"));
		assertEquals("2", storage.ip("three"));

		assertTrue(Arrays.asList("1", "2", "3").containsAll(storage.allIps()));
		assertTrue(Arrays.asList("1", "2", "3").contains(storage.anyIp()));

		storage.addLogin("one", "direction");
		assertEquals("2", storage.ip("one"));

		storage.addDataLocation("direction", "4");
		assertEquals("4", storage.ip("one"));
	}

	@Test
	public void testNullDataLocationForLogin() {
		HashMapProxyInfoStorage storage = new HashMapProxyInfoStorage();
		storage.addLogin("one", null);

		assertEquals(storage.ip("one"), null);
	}

	@Test
	public void testNoDataLocation() {
		HashMapProxyInfoStorage storage = new HashMapProxyInfoStorage();

		assertEquals(null, storage.ip("any"));
		assertEquals(null, storage.anyIp());
		assertEquals(Collections.EMPTY_LIST, storage.allIps());
	}
}
