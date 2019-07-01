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

package net.bluemind.backend.postfix;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.core.container.model.Container;
import net.bluemind.core.tests.vertx.VertxEventChecker;
import net.bluemind.group.hook.GroupMessage;
import net.bluemind.lib.vertx.VertxPlatform;

public class GroupHookTests {
	@Before
	public void before() throws Exception {
		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();
	}

	@Test
	public void onAddMembers() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new GroupHook().onAddMembers(new GroupMessage(null, null, new Container()));

		assertNotNull(dirtyMapChecker.shouldSuccess());
	}

	@Test
	public void onRemoveMembers() {
		VertxEventChecker<JsonObject> dirtyMapChecker = new VertxEventChecker<>("postfix.map.dirty");

		new GroupHook().onRemoveMembers(new GroupMessage(null, null, new Container()));

		assertNotNull(dirtyMapChecker.shouldSuccess());
	}
}
