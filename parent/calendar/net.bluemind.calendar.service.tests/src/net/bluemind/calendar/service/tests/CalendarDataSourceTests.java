/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.calendar.api.ICalendar;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.persistence.UserSubscriptionStore;

public class CalendarDataSourceTests extends AbstractCalendarTests {

	private Container wrongDsCalendar;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Before
	public void before() throws Exception {
		super.beforeBefore();

		String uid = "wrong-ds-calendar:" + testUser.uid;
		ContainerStore containerHome = new ContainerStore(testContext, JdbcActivator.getInstance().getDataSource(),
				userSecurityContext);
		wrongDsCalendar = Container.create(uid, ICalendarUids.TYPE, "wrong ds cal", testUser.uid, domainUid, true);
		wrongDsCalendar = containerHome.create(wrongDsCalendar);

		ContainerStore directoryStore = new ContainerStore(testContext, JdbcActivator.getInstance().getDataSource(),
				userSecurityContext);
		directoryStore.createContainerLocation(wrongDsCalendar, null);

		UserSubscriptionStore userSubcriptionStore = new UserSubscriptionStore(userSecurityContext,
				JdbcActivator.getInstance().getDataSource(), domainContainer);
		userSubcriptionStore.subscribe(userSecurityContext.getSubject(), wrongDsCalendar);
	}

	@Test
	public void testWrongDataSource() throws ServerFault {

		try {
			getCalendarService(userSecurityContext, wrongDsCalendar);
			fail("service init with wrong ds");
		} catch (ServerFault e) {
			assertEquals("wrong datasource", e.getMessage());
		}

		try {
			ServerSideServiceProvider.getProvider(userSecurityContext).instance(ICalendar.class, wrongDsCalendar.uid);
			fail("service init with wrong ds");
		} catch (ServerFault e) {
			assertEquals("wrong datasource", e.getMessage());
		}

	}

}
