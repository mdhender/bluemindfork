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
package net.bluemind.eas.http.tests.synchronization;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.http.tests.AbstractEasTest;
import net.bluemind.eas.http.tests.builders.CalendarBuilder;
import net.bluemind.eas.http.tests.helpers.CoreCalendarHelper;
import net.bluemind.eas.http.tests.helpers.SyncHelper;
import net.bluemind.eas.http.tests.helpers.SyncRequest;
import net.bluemind.eas.http.tests.helpers.SyncRequest.SyncRequestBuilder;
import net.bluemind.icalendar.api.ICalendarElement.RRule.Frequency;

public class CalendarSynchronizationTests extends AbstractEasTest {

	@Test
	public void testServerCreationSyncV16() throws Exception {
		long calId = CoreCalendarHelper.getUserCalendarId("user", domain.uid);

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		new SyncHelper.SyncHelperBuilder() //
				.withAuth(latd, password) //
				.withCollectionId(calId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation() //
				.execute(CoreCalendarHelper::addEvent) //
				.sync(request) //
				.startValidation() //
				.assertSyncKeyChanged() //
				.assertNamespace("Location", "AirSyncBase") //
				.assertNamespace("DisplayName", "AirSyncBase") //
				.endValidation() //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation();
	}

	@Test
	public void testServerCreationSyncV14() throws Exception {
		long calId = CoreCalendarHelper.getUserCalendarId("user", domain.uid);

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		new SyncHelper.SyncHelperBuilder() //
				.withAuth(latd, password) //
				.withCollectionId(calId) //
				.withProtocolVersion(ProtocolVersion.V141).build() //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation() //
				.execute(CoreCalendarHelper::addEvent) //
				.sync(request) //
				.startValidation() //
				.assertSyncKeyChanged() //
				.assertNamespace("Location", "Calendar") //
				.assertMissingElement("DisplayName") //
				.endValidation() //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation();
	}

	@Test
	public void testClientCreationSyncV16() throws Exception {
		long calId = CoreCalendarHelper.getUserCalendarId("user", domain.uid);

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		new SyncHelper.SyncHelperBuilder() //
				.withAuth(latd, password) //
				.withCollectionId(calId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.execute(CoreCalendarHelper.validateEventCount(0)) //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation() //
				.sync(request.copy() //
						.withClientChangesAdd(CalendarBuilder.getSimpleEvent(ProtocolVersion.V161) //
						).build()) //
				.startValidation() //
				.assertServerConfirmation(1) //
				.endValidation() //
				.execute(CoreCalendarHelper.validateEventCount(1)) //
				.execute(CoreCalendarHelper.validateDefaultEvent(ProtocolVersion.V161,
						CoreCalendarHelper.eventBySummary("event")));
	}

	@Test
	public void testClientCreationSyncV14() throws Exception {
		long calId = CoreCalendarHelper.getUserCalendarId("user", domain.uid);

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		new SyncHelper.SyncHelperBuilder() //
				.withAuth(latd, password) //
				.withCollectionId(calId) //
				.withProtocolVersion(ProtocolVersion.V141).build() //
				.execute(CoreCalendarHelper.validateEventCount(0)) //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation() //
				.sync(request.copy() //
						.withClientChangesAdd(CalendarBuilder.getSimpleEvent(ProtocolVersion.V141) //
						).build()) //
				.startValidation() //
				.assertServerConfirmation(1) //
				.endValidation() //
				.execute(CoreCalendarHelper.validateEventCount(1)) //
				.execute(CoreCalendarHelper.validateDefaultEvent(ProtocolVersion.V141,
						CoreCalendarHelper.eventBySummary("event")));
	}

	@Test
	public void testClientDeleteEventOccurrenceSyncV16() throws Exception {
		long calId = CoreCalendarHelper.getUserCalendarId("user", domain.uid);
		String eventUid = UUID.randomUUID().toString();
		AtomicLong eventItemId = new AtomicLong();

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		new SyncHelper.SyncHelperBuilder() //
				.withAuth(latd, password) //
				.withCollectionId(calId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.execute(CoreCalendarHelper.validateEventCount(0)) //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation() //
				.execute(() -> CoreCalendarHelper.addEvent(eventUid,
						CalendarBuilder.Builder.defaultEventBuilder().withReccurrence(Frequency.DAILY)
								.withException(3, 2).get())) //
				.execute(() -> CoreCalendarHelper.eventByUid(eventUid, series -> eventItemId.set(series.internalId))) //
				.sync(request) //
				.startValidation() //
				.assertSyncKeyChanged() //
				.assertElementText("0", "Recurrence", "Type") //
				.assertElementText("0", "Exceptions", "Exception", "Deleted") //
				.assertElementText("20220216T090000Z", "Exceptions", "Exception", "ExceptionStartTime") //
				.assertElementText("20220216T110000Z", "Exceptions", "Exception", "StartTime") //
				.assertElementText("20220216T130000Z", "Exceptions", "Exception", "EndTime") //
				.endValidation() //
				.sync(new SyncRequestBuilder() //
						.withClientChangesDeleteOccurrence(calId + ":" + eventItemId, "20220216T090000Z") //
						.build()) //
				.execute(CoreCalendarHelper.validateExDate(eventUid, "20220216T090000Z"));
	}

}
