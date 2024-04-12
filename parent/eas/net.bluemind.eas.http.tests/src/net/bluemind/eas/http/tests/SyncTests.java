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
package net.bluemind.eas.http.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.bluemind.eas.client.ProtocolVersion;
import net.bluemind.eas.http.tests.helpers.CoreCalendarHelper;
import net.bluemind.eas.http.tests.helpers.SyncHelper;
import net.bluemind.eas.http.tests.helpers.SyncRequest;
import net.bluemind.eas.http.tests.helpers.SyncRequest.SyncRequestBuilder;

public class SyncTests extends AbstractEasTest {

	@Test
	public void testSimpleCalendarSyncV16() throws Exception {
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
				.endValidation() //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation();
	}

	@Test
	public void testSimpleCalendarSyncV14() throws Exception {
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
				.endValidation() //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation();
	}

	@Test
	public void testCalendarSyncClientChangesV16() throws Exception {
		long calId = CoreCalendarHelper.getUserCalendarId("user", domain.uid);

		SyncRequest request = new SyncRequestBuilder().withChanges().build();
		new SyncHelper.SyncHelperBuilder() //
				.withAuth(latd, password) //
				.withCollectionId(calId) //
				.withProtocolVersion(ProtocolVersion.V161).build() //
				.execute(this.validateEventCount(0)) //
				.sync(request) //
				.startValidation() //
				.assertEmptyResponse() //
				.endValidation() //
				.sync(request.copy() //
						.withClientChangesAdd(CoreCalendarHelper.getClientEventData(ProtocolVersion.V161) //
						).build()) //
				.startValidation() //
				.assertEventConfirmation(1) //
				.endValidation() //
				.execute(this.validateEventCount(1));
	}

	private Runnable validateEventCount(int count) {
		return () -> assertEquals(count, CoreCalendarHelper.getAllEvents().size());
	}

}
