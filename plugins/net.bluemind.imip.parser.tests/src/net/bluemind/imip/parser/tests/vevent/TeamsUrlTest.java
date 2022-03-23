/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.tests.IMIPTestCase;
import net.bluemind.videoconferencing.teams.TeamsHeaders;

public class TeamsUrlTest extends IMIPTestCase {

	public void testNewInvite() throws Exception {
		Message m = parseData("outlook-teams-invite.eml");
		assertNotNull(m);

		IMIPInfos infos = IMIPParserFactory.create().parse(m);
		assertNotNull(infos);
		assertEquals(
				"https://teams.microsoft.com/l/meetup-join/19%3ameeting_NDg4MmE1ZGEtMDlkMi00YTIxLTljMDktNjljZjZlMDE2YzU5%40thread.v2/0?context=%7b%22Tid%22%3a%224cd23f0c-c521-459e-9efc-cfe000f856ac%22%2c%22Oid%22%3a%22f314f192-544c-4a91-af5e-7cd2ef9df199%22%7d",
				infos.iCalendarElements.get(0).conference);
		assertEquals(TeamsHeaders.MICROSOFT_TEAMS_CONFERENCE_ID, infos.iCalendarElements.get(0).conferenceId);
	}

}
