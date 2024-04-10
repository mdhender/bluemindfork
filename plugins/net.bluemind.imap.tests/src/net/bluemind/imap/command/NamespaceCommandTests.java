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
package net.bluemind.imap.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.bluemind.imap.NameSpaceInfo;
import net.bluemind.imap.impl.IMAPResponse;

public class NamespaceCommandTests {

	private NamespaceCommand command;

	@BeforeEach
	public void setup() {
		command = new NamespaceCommand();
	}

	@Test
	public void testParsingRFC2342Ex5Dot1() {
		IMAPResponse response = createImapResponseFromPayload("* NAMESPACE ((\"\" \"/\")) NIL NIL");
		command.responseReceived(Arrays.asList(response));
		NameSpaceInfo namespaceInfo = command.getReceivedData();
		assertEquals(1, namespaceInfo.getPersonal().size());
		assertEquals(0, namespaceInfo.getOtherUsers().size());
		assertEquals(0, namespaceInfo.getMailShares().size());
		assertEquals("", namespaceInfo.getPersonal().get(0));
	}

	@Test
	public void testParsingRFC2342Ex5Dot2() {
		IMAPResponse response = createImapResponseFromPayload("* NAMESPACE NIL NIL ((\"\" \".\"))");
		command.responseReceived(Arrays.asList(response));
		NameSpaceInfo namespaceInfo = command.getReceivedData();
		assertEquals(0, namespaceInfo.getPersonal().size());
		assertEquals(0, namespaceInfo.getOtherUsers().size());
		assertEquals(1, namespaceInfo.getMailShares().size());
		assertEquals("", namespaceInfo.getMailShares().get(0));
	}

	@Test
	public void testParsingRFC2342Ex5Dot3() {
		IMAPResponse response = createImapResponseFromPayload(
				"* NAMESPACE ((\"\" \"/\")) NIL ((\"Public Folders/\" \"/\"))");
		command.responseReceived(Arrays.asList(response));
		NameSpaceInfo namespaceInfo = command.getReceivedData();
		assertEquals(1, namespaceInfo.getPersonal().size());
		assertEquals(0, namespaceInfo.getOtherUsers().size());
		assertEquals(1, namespaceInfo.getMailShares().size());
		assertEquals("", namespaceInfo.getPersonal().get(0));
		assertEquals("Public Folders/", namespaceInfo.getMailShares().get(0));
	}

	// @Test
	// public void testParsingRFC2342Ex5Dot4() {
	// IMAPResponse response = createImapResponseFromPayload(
	// "* NAMESPACE ((\"\" \"/\")) ((\"~\" \"/\")) " +
	// "((\"#shared/\" \"/\") (\"#public/\" \"/\") " +
	// "(\"#ftp/\" \"/\")(\"#news.\" \".\"))");
	// command.responseReceived(Arrays.asList(response));
	// NameSpaceInfo namespaceInfo = command.getReceivedData();
	// assertEquals(1, namespaceInfo.getPersonal().size());
	// assertEquals(1, namespaceInfo.getOtherUsers().size());
	// assertEquals(4, namespaceInfo.getMailShares().size());
	// assertEquals("", namespaceInfo.getPersonal().get(0));
	// assertEquals("~", namespaceInfo.getOtherUsers().get(0));
	// assertEquals(Arrays.asList("#shared/", "#public/", "#ftp/",
	// "#news."),
	// namespaceInfo.getMailShares());
	// }

	@Test
	public void testParsingRFC2342Ex5Dot5() {
		IMAPResponse response = createImapResponseFromPayload("* NAMESPACE ((\"INBOX.\" \".\")) NIL  NIL");
		command.responseReceived(Arrays.asList(response));
		NameSpaceInfo namespaceInfo = command.getReceivedData();
		assertEquals(1, namespaceInfo.getPersonal().size());
		assertEquals(0, namespaceInfo.getOtherUsers().size());
		assertEquals(0, namespaceInfo.getMailShares().size());
		assertEquals("INBOX.", namespaceInfo.getPersonal().get(0));
	}

	// @Test
	// public void testParsingRFC2342Ex5Dot6() {
	// IMAPResponse response = createImapResponseFromPayload(
	// "* NAMESPACE ((\"\" \"/\")(\"#mh/\" \"/\" \"X-PARAM\" (\"FLAG1\"
	// \"FLAG2\"))) NIL NIL");
	// command.responseReceived(Arrays.asList(response));
	// NameSpaceInfo namespaceInfo = command.getReceivedData();
	// assertEquals(2, namespaceInfo.getPersonal().size());
	// assertEquals(0, namespaceInfo.getOtherUsers().size());
	// assertEquals(0, namespaceInfo.getMailShares().size());
	// assertEquals("", namespaceInfo.getPersonal().get(0));
	// assertEquals("#mh/", namespaceInfo.getPersonal().get(1));
	// }

	@Test
	public void testParsingRFC2342Ex5Dot7() {
		IMAPResponse response = createImapResponseFromPayload(
				"* NAMESPACE ((\"\" \"/\")) ((\"Other Users/\" \"/\")) NIL");
		command.responseReceived(Arrays.asList(response));
		NameSpaceInfo namespaceInfo = command.getReceivedData();
		assertEquals(1, namespaceInfo.getPersonal().size());
		assertEquals(1, namespaceInfo.getOtherUsers().size());
		assertEquals(0, namespaceInfo.getMailShares().size());
		assertEquals("", namespaceInfo.getPersonal().get(0));
		assertEquals("Other Users/", namespaceInfo.getOtherUsers().get(0));
	}

	@Test
	public void testParsingRFC2342Ex5Dot8() {
		IMAPResponse response = createImapResponseFromPayload("* NAMESPACE ((\"\" \"/\")) ((\"#Users/\" \"/\")) NIL");
		command.responseReceived(Arrays.asList(response));
		NameSpaceInfo namespaceInfo = command.getReceivedData();
		assertEquals(1, namespaceInfo.getPersonal().size());
		assertEquals(1, namespaceInfo.getOtherUsers().size());
		assertEquals(0, namespaceInfo.getMailShares().size());
		assertEquals("", namespaceInfo.getPersonal().get(0));
		assertEquals("#Users/", namespaceInfo.getOtherUsers().get(0));
	}

	@Test
	public void testParsingRFC2342Ex5Dot9() {
		IMAPResponse response = createImapResponseFromPayload("* NAMESPACE ((\"\" \"/\")) ((\"~\" \"/\")) NIL");
		command.responseReceived(Arrays.asList(response));
		NameSpaceInfo namespaceInfo = command.getReceivedData();
		assertEquals(1, namespaceInfo.getPersonal().size());
		assertEquals(1, namespaceInfo.getOtherUsers().size());
		assertEquals(0, namespaceInfo.getMailShares().size());
		assertEquals("", namespaceInfo.getPersonal().get(0));
		assertEquals("~", namespaceInfo.getOtherUsers().get(0));
	}

	@Test
	public void testParsingRFC2342UTF7() {
		IMAPResponse response = createImapResponseFromPayload(
				"* NAMESPACE ((\"Bo&AO4-tes partag&AOk-es/\" \"/\")) NIL NIL");
		command.responseReceived(Arrays.asList(response));
		NameSpaceInfo namespaceInfo = command.getReceivedData();
		assertEquals(0, namespaceInfo.getMailShares().size());
		assertEquals(0, namespaceInfo.getOtherUsers().size());
		assertEquals(1, namespaceInfo.getPersonal().size());
		assertEquals("Boîtes partagées/", namespaceInfo.getPersonal().get(0));
	}

	private IMAPResponse createImapResponseFromPayload(String payload) {
		IMAPResponse imapResponse = new IMAPResponse();
		imapResponse.setStatus("OK");
		imapResponse.setPayload(payload);
		return imapResponse;
	}

}
