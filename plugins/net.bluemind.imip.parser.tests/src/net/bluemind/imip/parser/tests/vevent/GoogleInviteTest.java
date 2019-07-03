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

import net.bluemind.core.api.date.BmDateTime;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.ITIPMethod;
import net.bluemind.imip.parser.tests.IMIPTestCase;

public class GoogleInviteTest extends IMIPTestCase {

	public void testNewInvite() throws Exception {
		Message m = parseData("google.invite.new.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertEquals(ITIPMethod.REQUEST, infos.method);
		BmDateTime v = infos.iCalendarElements.get(0).dtstart;
		assertEquals("UTC", v.timezone);
		assertEquals("2012-05-30T09:00:00.000Z", v.iso8601);
	}

}
