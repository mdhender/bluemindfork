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
package net.bluemind.announcement.service.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.util.concurrent.SettableFuture;

import net.bluemind.announcement.api.Announcement;
import net.bluemind.announcement.api.Announcement.Target;
import net.bluemind.announcement.api.IUserAnnouncements;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;

public class UserAnnouncementsServiceTests {

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		

		final SettableFuture<Void> future = SettableFuture.<Void> create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void setAndGetMessage() throws ServerFault {
		IUserAnnouncements um = service(SecurityContext.SYSTEM);
		List<Announcement> messages = um.get();
		int messageCount = messages.size();

		AnnouncementProvider.announcements
				.add(Announcement.create(Target.All, Announcement.Kind.Info, "y'a plus de café", true));

		messages = um.get();
		assertEquals(messageCount + 1, messages.size());

		AnnouncementProvider.announcements
				.add(Announcement.create(Target.All, Announcement.Kind.Error, "y'a plus de café, bordel !", true));

		messages = um.get();
		assertEquals(messageCount + 2, messages.size());

	}

	public IUserAnnouncements service(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(context).instance(IUserAnnouncements.class, context.getSubject());
	}

}
