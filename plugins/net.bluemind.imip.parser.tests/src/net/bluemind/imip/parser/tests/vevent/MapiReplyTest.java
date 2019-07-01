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

import java.util.List;

import org.apache.james.mime4j.dom.Message;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.imip.parser.tests.IMIPTestCase;

public class MapiReplyTest extends IMIPTestCase {

	public void testReplyFromOutlookWithMapi() throws Exception {
		Message m = parseData("bm4.mapi.reply.accept.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertEquals(ITIPMethod.REPLY, infos.method);

		assertNotNull(infos.uid);
		assertNotNull(infos.iCalendarElements);
		VEvent ev = (VEvent) infos.iCalendarElements.get(0);
		List<VEvent.Attendee> atts = ev.attendees;
		assertEquals(1, atts.size());
		VEvent.Attendee at = atts.get(0);
		System.err.println("attendee " + at);
		assertEquals(VEvent.ParticipationStatus.Accepted, at.partStatus);
	}

}
