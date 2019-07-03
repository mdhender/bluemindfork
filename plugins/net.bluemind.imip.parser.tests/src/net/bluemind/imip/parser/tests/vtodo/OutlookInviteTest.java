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

import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.imip.parser.tests.IMIPTestCase;

public class OutlookInviteTest extends IMIPTestCase {

	public void testNewInvite() throws Exception {
		Message m = parseData("outlook.task.invite.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);

		assertEquals(ITIPMethod.REQUEST, infos.method);
		ICalendarElement elem = infos.iCalendarElements.get(0);
		assertEquals("test tâche", elem.summary);
		assertNotNull(elem.dtstart);
		assertEquals(2015, new BmDateTimeWrapper(elem.dtstart).toJodaTime().getYear());
		assertEquals(22, new BmDateTimeWrapper(elem.dtstart).toJodaTime().getDayOfMonth());
		assertEquals(7, new BmDateTimeWrapper(elem.dtstart).toJodaTime().getMonthOfYear());

	}

}
