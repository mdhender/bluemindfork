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
package net.bluemind.imip.parser.tests.vevent;

import org.apache.james.mime4j.dom.Message;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.imip.parser.tests.IMIPTestCase;

public class ExchangeInviteTest extends IMIPTestCase {

	public void testInviteToAlias() throws Exception {
		Message m = parseData("exchange.invite.to_alias.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertEquals(ITIPMethod.REQUEST, infos.method);

		VEvent ev = (VEvent) infos.iCalendarElements.get(0);
		for (VEvent.Attendee at : ev.attendees) {
			System.out.println("Attendee: " + at.mailto);
		}
	}

	public void testInviteStrangeTimezone() throws Exception {
		Message m = parseData("exanchage.invite.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertEquals(ITIPMethod.REQUEST, infos.method);

		VEvent ev = (VEvent) infos.iCalendarElements.get(0);
		assertEquals("Europe/Paris", ev.dtstart.timezone);
	}

	public void testInviteWithEmailInUpperCase() throws Exception {
		Message m = parseData("exanchage.invite_email_uppercase.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertEquals(ITIPMethod.REQUEST, infos.method);

		VEvent ev = (VEvent) infos.iCalendarElements.get(0);
		assertEquals(1, ev.attendees.size());
		assertEquals("tom@willow.vmw", ev.attendees.get(0).mailto);
	}

}
