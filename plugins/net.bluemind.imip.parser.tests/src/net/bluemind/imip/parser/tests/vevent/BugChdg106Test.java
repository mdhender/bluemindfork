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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.apache.james.mime4j.dom.TextBody;

import net.bluemind.calendar.api.VEvent;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.imip.parser.tests.IMIPTestCase;

public class BugChdg106Test extends IMIPTestCase {

	public void testIsReccurring() throws Exception {
		Message m = parseData("bug_chdg-106.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertEquals(ITIPMethod.REQUEST, infos.method);
		VEvent ev = (VEvent) infos.iCalendarElements.get(0);
		VEvent.RRule rec = ev.rrule;
		assertNotNull(rec);
		System.out.println("rec.kind: " + rec.frequency);
		// dumpIcsContent(m);

		System.out.println("Rec ends: " + rec.until);
	}

	public void dumpIcsContent(Message m) throws IOException {
		List<Entity> parts = ((Multipart) m.getBody()).getBodyParts();

		Entity calPart = null;
		for (Entity e : parts) {
			System.out.println("On " + e.getMimeType());
			if ("text/calendar".equals(e.getMimeType())) {
				calPart = e;
			}
		}
		assertNotNull(calPart);
		TextBody body = (TextBody) calPart.getBody();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		body.writeTo(out);
		String icsString = out.toString();
		System.out.println("--------- ICS ---------- \n" + icsString);
	}
}
