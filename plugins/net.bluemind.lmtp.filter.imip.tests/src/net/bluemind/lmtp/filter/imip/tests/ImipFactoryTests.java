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
package net.bluemind.lmtp.filter.imip.tests;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.lmtp.filter.imip.EventCancelHandler;
import net.bluemind.lmtp.filter.imip.EventReplyHandler;
import net.bluemind.lmtp.filter.imip.EventRequestHandler;
import net.bluemind.lmtp.filter.imip.IIMIPHandler;
import net.bluemind.lmtp.filter.imip.IMIPHandlerFactory;
import net.bluemind.lmtp.filter.imip.TodoCancelHandler;
import net.bluemind.lmtp.filter.imip.TodoReplyHandler;
import net.bluemind.lmtp.filter.imip.TodoRequestHandler;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.todolist.api.VTodo;

public class ImipFactoryTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		PopulateHelper.initGlobalVirt();
		PopulateHelper.addDomainAdmin("admin0", "global.virt");

		PopulateHelper.createTestDomain(System.currentTimeMillis() + ".loc");
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testEventRequestHandler() {
		IMIPInfos info = new IMIPInfos();
		info.method = ITIPMethod.REQUEST;
		info.iCalendarElements = Arrays.asList(new VEvent());

		IIMIPHandler handler = IMIPHandlerFactory.get(info, null, null);

		assertTrue(handler instanceof EventRequestHandler);
	}

	@Test
	public void testEventCancelHandler() {
		IMIPInfos info = new IMIPInfos();
		info.method = ITIPMethod.CANCEL;
		info.iCalendarElements = Arrays.asList(new VEvent());

		IIMIPHandler handler = IMIPHandlerFactory.get(info, null, null);

		assertTrue(handler instanceof EventCancelHandler);
	}

	@Test
	public void testEventReplyHandler() {
		IMIPInfos info = new IMIPInfos();
		info.method = ITIPMethod.REPLY;
		info.iCalendarElements = Arrays.asList(new VEvent());

		IIMIPHandler handler = IMIPHandlerFactory.get(info, null, null);

		assertTrue(handler instanceof EventReplyHandler);
	}

	@Test
	public void testTodoRequestHandler() {
		IMIPInfos info = new IMIPInfos();
		info.method = ITIPMethod.REQUEST;
		info.iCalendarElements = Arrays.asList(new VTodo());

		IIMIPHandler handler = IMIPHandlerFactory.get(info, null, null);

		assertTrue(handler instanceof TodoRequestHandler);
	}

	@Test
	public void testTodoCancelHandler() {
		IMIPInfos info = new IMIPInfos();
		info.method = ITIPMethod.CANCEL;
		info.iCalendarElements = Arrays.asList(new VTodo());

		IIMIPHandler handler = IMIPHandlerFactory.get(info, null, null);

		assertTrue(handler instanceof TodoCancelHandler);
	}

	@Test
	public void testTodoReplyHandler() {
		IMIPInfos info = new IMIPInfos();
		info.method = ITIPMethod.REPLY;
		info.iCalendarElements = Arrays.asList(new VTodo());

		IIMIPHandler handler = IMIPHandlerFactory.get(info, null, null);

		assertTrue(handler instanceof TodoReplyHandler);

	}

	@Test
	public void testInvalidArgument() {
		IMIPInfos info = new IMIPInfos();
		info.method = ITIPMethod.ADD;
		info.iCalendarElements = Arrays.asList(new VTodo());

		IIMIPHandler handler = IMIPHandlerFactory.get(info, null, null);

		assertNull(handler);
	}

	@Test
	public void testNoDomainObject() {
		IMIPInfos info = new IMIPInfos();
		info.method = ITIPMethod.ADD;

		IIMIPHandler handler = IMIPHandlerFactory.get(info, null, null);

		assertNull(handler);
	}

}
