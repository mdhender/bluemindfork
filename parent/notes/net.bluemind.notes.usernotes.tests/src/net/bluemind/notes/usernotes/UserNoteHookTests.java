/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.notes.usernotes;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.notes.api.INote;
import net.bluemind.notes.api.INoteUids;
import net.bluemind.notes.api.VNote;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;

public class UserNoteHookTests {

	private String domainUid;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

		ElasticsearchTestHelper.getInstance().beforeTest();
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");
		PopulateHelper.initGlobalVirt(esServer);

		domainUid = "dom" + System.currentTimeMillis() + ".test";
		domainUid = PopulateHelper.createTestDomain(domainUid).uid;
	}

	@After
	public void teardown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testUserActionHook() throws Exception {
		String userUid = PopulateHelper.addUser("user1", domainUid);

		IContainers containerService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IContainers.class);
		assertNotNull(containerService.get(INoteUids.defaultUserNotes("user1")));

		INote noteService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(INote.class,
				INoteUids.defaultUserNotes("user1"));

		noteService.create("2", defaultVNote());

		assertNotNull(noteService.getComplete("2"));

		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		TaskUtils.wait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), userService.delete(userUid));

		assertNull(containerService.getIfPresent(INoteUids.defaultUserNotes("user1")));
	}

	protected VNote defaultVNote() {
		VNote note = new VNote();
		note.subject = "Note " + System.currentTimeMillis();
		note.body = "Content";
		note.height = 25;
		note.width = 42;
		note.posX = 25;
		note.posY = 42;

		return note;
	}

}
