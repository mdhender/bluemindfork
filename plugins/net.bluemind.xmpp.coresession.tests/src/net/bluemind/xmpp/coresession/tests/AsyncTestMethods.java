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
package net.bluemind.xmpp.coresession.tests;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

public class AsyncTestMethods {

	private static final Object NULL = new Object();
	private ConcurrentHashMap<String, LinkedBlockingDeque<Object>> assertMap = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	protected <T> T waitAssert(String key, long timeout, TimeUnit unit) {
		assertMap.putIfAbsent(key, new LinkedBlockingDeque<>());
		try {
			Object ret = assertMap.get(key).poll(timeout, unit);
			if (ret == NULL) {
				return null;
			} else {
				return (T) ret;
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	protected <T> T waitAssert(String key) {
		return waitAssert(key, 2, TimeUnit.SECONDS);
	}

	protected void dequeueAllAssert(String key) {
		assertMap.putIfAbsent(key, new LinkedBlockingDeque<>());
		LinkedBlockingDeque<Object> queue = assertMap.get(key);
		queue.clear();
	}

	protected void queueAssertValue(String key, Object value) {
		assertMap.putIfAbsent(key, new LinkedBlockingDeque<>());

		if (value != null) {
			assertMap.get(key).add(value);
		} else {
			assertMap.get(key).add(NULL);
		}
	}
}
