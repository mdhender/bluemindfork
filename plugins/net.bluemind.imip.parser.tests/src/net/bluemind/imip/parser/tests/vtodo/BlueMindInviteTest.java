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
package net.bluemind.imip.parser.tests.vtodo;

import org.apache.james.mime4j.dom.Message;

import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.imip.parser.tests.IMIPTestCase;
import net.bluemind.todolist.api.VTodo;

public class BlueMindInviteTest extends IMIPTestCase {

	public void testNewInvite() throws Exception {
		Message m = parseData("bm.task.invite.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertEquals(ITIPMethod.REQUEST, infos.method);
	}

	public void testInviteUpdate() throws Exception {
		Message m = parseData("bm.task.update.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertTrue("sequence must be > 0 in the ICS from an event update", infos.sequence > 0);
		assertEquals(ITIPMethod.REQUEST, infos.method);
	}

	public void testInviteCancel() throws Exception {
		Message m = parseData("bm.task.cancel.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertEquals(ITIPMethod.CANCEL, infos.method);

		VTodo ev = (VTodo) infos.iCalendarElements.get(0);
		assertNotNull(ev);
		String ownerEmail = ev.organizer.mailto;
		System.out.println("ev.ownerEmail: " + ownerEmail);
		String organizerEmail = infos.organizerEmail;
		assertNotNull(organizerEmail);
	}

}
