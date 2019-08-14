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
package net.bluemind.calendar.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Test;

import net.bluemind.calendar.api.IPublishCalendar;
import net.bluemind.calendar.api.PublishMode;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.service.AbstractCalendarTests;
import net.bluemind.calendar.service.internal.PublishCalendarService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.tests.defaultdata.BmDateTimeHelper;

public class PublishCalendarTests extends AbstractCalendarTests {

	@Test
	public void publishCalendarPublic() throws Exception {
		createEvents();

		try {
			getPublishCalendarService(userSecurityContext, userCalendarContainer).publish("no-no");
			fail();
		} catch (ServerFault sf) {
		}

		String url = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.generateUrl(PublishMode.PUBLIC);
		String token = url.substring(url.indexOf("x-calendar"));

		String export = GenericStream
				.streamToString(getPublishCalendarService(userSecurityContext, userCalendarContainer).publish(token));
		assertNotNull(export);
		assertTrue(export.contains("X-WR-CALNAME:John Doe"));
		assertTrue(export.contains("LOCATION:BlueMind"));
		assertTrue(export.contains("DESCRIPTION:bla bla"));
		assertTrue(export.contains("SUMMARY:wOOt"));

		assertFalse(export.contains("LOCATION:xxx"));
		assertFalse(export.contains("DESCRIPTION:aaa"));
		assertFalse(export.contains("SUMMARY:top secret"));

		aclStore.store(userCalendarContainer, Arrays.asList());

		try {
			getPublishCalendarService(userSecurityContext, userCalendarContainer).publish(token);
			fail();
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void publishCalendarPrivate() throws Exception {
		createEvents();

		try {
			getPublishCalendarService(userSecurityContext, userCalendarContainer).publish("no-no");
			fail();
		} catch (ServerFault sf) {
		}

		String url = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.generateUrl(PublishMode.PRIVATE);
		String token = url.substring(url.indexOf("x-calendar"));

		String export = GenericStream
				.streamToString(getPublishCalendarService(userSecurityContext, userCalendarContainer).publish(token));

		assertNotNull(export);
		assertTrue(export.contains("X-WR-CALNAME:John Doe"));
		assertTrue(export.contains("LOCATION:BlueMind"));
		assertTrue(export.contains("DESCRIPTION:bla bla"));
		assertTrue(export.contains("SUMMARY:wOOt"));

		assertTrue(export.contains("LOCATION:xxx"));
		assertTrue(export.contains("DESCRIPTION:aaa"));
		assertTrue(export.contains("SUMMARY:top secret"));

	}

	@Test
	public void testGettingSharedUrls() throws Exception {
		createEvents();

		String url1 = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.generateUrl(PublishMode.PRIVATE);
		String url2 = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.generateUrl(PublishMode.PRIVATE);
		String url3 = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.generateUrl(PublishMode.PUBLIC);

		List<String> generatedUrlsPrivate = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.getGeneratedUrls(PublishMode.PRIVATE);
		List<String> generatedUrlsPublic = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.getGeneratedUrls(PublishMode.PUBLIC);

		assertEquals(2, generatedUrlsPrivate.size());
		assertEquals(1, generatedUrlsPublic.size());

		assertTrue(generatedUrlsPrivate.contains(url1));
		assertTrue(generatedUrlsPrivate.contains(url2));
		assertTrue(generatedUrlsPublic.contains(url3));
	}

	@Test
	public void testDisableSharedUrl() throws Exception {
		createEvents();

		String url1 = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.generateUrl(PublishMode.PRIVATE);
		String url2 = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.generateUrl(PublishMode.PRIVATE);
		String url3 = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.generateUrl(PublishMode.PUBLIC);

		List<String> generatedUrlsPrivate = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.getGeneratedUrls(PublishMode.PRIVATE);
		List<String> generatedUrlsPublic = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.getGeneratedUrls(PublishMode.PUBLIC);

		assertEquals(2, generatedUrlsPrivate.size());
		assertEquals(1, generatedUrlsPublic.size());

		getPublishCalendarService(userSecurityContext, userCalendarContainer).disableUrl(url2);
		getPublishCalendarService(userSecurityContext, userCalendarContainer).disableUrl(url3);

		generatedUrlsPrivate = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.getGeneratedUrls(PublishMode.PRIVATE);
		generatedUrlsPublic = getPublishCalendarService(userSecurityContext, userCalendarContainer)
				.getGeneratedUrls(PublishMode.PUBLIC);

		assertEquals(1, generatedUrlsPrivate.size());
		assertEquals(0, generatedUrlsPublic.size());

		assertTrue(generatedUrlsPrivate.contains(url1));
	}

	private void createEvents() throws ServerFault {
		VEventSeries event = defaultVEvent();
		event.main.classification = Classification.Public;
		event.main.summary = "wOOt";
		event.main.location = "BlueMind";
		event.main.description = "bla bla";
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 4, 17, 0, 0, 0, defaultTz));
		event.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 4, 18, 0, 0, 0, defaultTz));
		getCalendarService(userSecurityContext, userCalendarContainer).create(UUID.randomUUID().toString(), event,
				sendNotifications);

		event = defaultVEvent();
		event.main.classification = Classification.Private;
		event.main.summary = "top secret";
		event.main.location = "xxx";
		event.main.description = "aaa";
		event.main.dtstart = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 4, 17, 0, 0, 0, defaultTz));
		event.main.dtend = BmDateTimeHelper.time(ZonedDateTime.of(2017, 2, 4, 18, 0, 0, 0, defaultTz));
		getCalendarService(userSecurityContext, userCalendarContainer).create(UUID.randomUUID().toString(), event,
				sendNotifications);

	}

	protected IPublishCalendar getPublishCalendarService(SecurityContext context, Container container)
			throws ServerFault {
		BmContext bmContext = new BmTestContext(context);
		DataSource ds = DataSourceRouter.get(bmContext, container.uid);
		return new PublishCalendarService(bmContext, ds, container);
	}

}
