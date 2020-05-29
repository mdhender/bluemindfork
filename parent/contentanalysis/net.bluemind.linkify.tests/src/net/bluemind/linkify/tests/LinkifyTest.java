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
package net.bluemind.linkify.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.bluemind.linkify.Linkify;

public class LinkifyTest {

	String teamsMeetingBody = "________________________________________________________________________________\n"
			+ "Join Microsoft Teams Meeting<https://teams.microsoft.com/l/meetup-join/19%3ameeting_NTRiMGQ0MmEtZjBmYy00MzgyLWIwZDAtNDNiZWY2ZTA0OTBh%40thread.v2/0?context=%7b%22Tid%22%3a%224cd23f0c-c521-459e-9efc-cfe000f856ac%22%2c%22Oid%22%3a%22f314f192-544c-4a91-af5e-7cd2ef9df199%22%7d>\n"
			+ "Learn more about Teams<https://aka.ms/JoinTeamsMeeting> | Meeting options<https://teams.microsoft.com/meetingOptions/?organizerId=f314f192-544c-4a91-af5e-7cd2ef9df199&tenantId=4cd23f0c-c521-459e-9efc-cfe000f856ac&threadId=19_meeting_NTRiMGQ0MmEtZjBmYy00MzgyLWIwZDAtNDNiZWY2ZTA0OTBh@thread.v2&messageId=0&language=en-US>\n"
			+ "________________________________________________________________________________\n";

	String phoneAndMails = "J'ai pas tinder.com mais tu peux m'envoyer un mail sur roberto@bluemind.net ou un sexto au 06 42 25 92 39";

	@Test
	public void testTeamsBody() {
		String html = Linkify.toHtml(teamsMeetingBody);
		System.err.println("HTML:\n" + html);
		assertNotNull(html);
		assertTrue(html.contains("<br>"));
		assertTrue(html.contains("href=\"https://aka.ms"));
	}

	@Test
	public void testMailAndPhone() {
		String html = Linkify.toHtml(phoneAndMails);
		System.err.println("IN:\n" + phoneAndMails + "\nHTML:\n" + html);
		assertNotNull(html);
		assertTrue(html.contains("href"));
	}

}
