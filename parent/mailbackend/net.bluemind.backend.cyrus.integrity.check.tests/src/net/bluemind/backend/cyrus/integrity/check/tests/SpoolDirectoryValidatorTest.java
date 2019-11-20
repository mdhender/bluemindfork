/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.cyrus.integrity.check.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.backend.cyrus.integrity.check.MailboxEntry;
import net.bluemind.backend.cyrus.integrity.check.SpoolDirectoryValidator;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.mailbox.api.Mailbox.Type;

public class SpoolDirectoryValidatorTest {

	private SpoolDirectoryValidator sdv;

	@Before
	public void before() {
		List<CyrusPartition> parts = Arrays.asList(//
				CyrusPartition.forServerAndDomain("public1", "face.bok"), //
				CyrusPartition.forServerAndDomain("public2", "twi.ter"), //
				CyrusPartition.forServerAndDomain("public2", "20minutes.fr")//
		);
		List<MailboxEntry> me = Arrays.asList(//
				new MailboxEntry(Type.user, "j.d", "face.bok"), //
				new MailboxEntry(Type.user, "tom", "face.bok"), //
				new MailboxEntry(Type.user, "50shades", "20minutes.fr"), //
				new MailboxEntry(Type.group, "_user", "twi.ter"), //
				new MailboxEntry(Type.mailshare, "bang.bus", "face.bok")//
		);
		sdv = SpoolDirectoryValidator.builder()//
				.validPartitions(parts)//
				.backendEntries(me)//
				.build();
	}

	@Test
	public void testValidatePrefix() {
		assertTrue(sdv.verify("public1__face_bok/domain/f/face.bok"));
		assertFalse(sdv.verify("public1__face_bok/domain/f/face_bok"));
		assertFalse(sdv.verify("public1__face_bok/domain/F/face.bok"));
	}

	@Test
	public void testUserMailbox() {
		assertTrue(sdv.verify("public1__face_bok/domain/f/face.bok/j/user/j^d"));
		assertFalse(sdv.verify("public1__face_bok/domain/f/face.bok/j/user/j.d"));
		assertTrue(sdv.verify("public2__20minutes_fr/domain/q/20minutes.fr/q/user/50shades"));
		assertFalse(sdv.verify("public2__20minutes_fr/domain/q/20minutes.fr/5/user/50shades"));
		assertFalse(sdv.verify("public1__face_bok/domain/f/face.bok/q/user/50shades"));
	}

	@Test
	public void testSharedMailbox() {
		assertTrue(sdv.verify("public1__face_bok/domain/f/face.bok/b/bang^bus"));
		assertFalse(sdv.verify("public1__face_bok/domain/f/face.bok/b/bang.bus"));
		assertTrue(sdv.verify("public1__face_bok/domain/f/face.bok/s/bang^bus/Sent"));
		assertFalse(sdv.verify("public1__face_bok/domain/f/face.bok/j/bang^bus/Sent"));
		assertFalse(sdv.verify("public1__face_bok/domain/f/face.bok/b/bang^bus/Sent"));
	}

	@Test
	public void testGroupMailbox() {
		assertTrue(sdv.verify("public2__twi_ter/domain/t/twi.ter/q/_user"));
		assertTrue(sdv.verify("public2__twi_ter/domain/t/twi.ter/u/_user/uranus"));
		assertFalse(sdv.verify("public1__face_bok/domain/f/face.bok/q/_user"));
	}

	@Test
	public void testNonLetterFirstChar() {
		assertFalse(sdv.verify("public2__20minutes_fr/domain/2/20minutes.fr"));
		assertTrue(sdv.verify("public2__20minutes_fr/domain/q/20minutes.fr"));
	}

	@Test
	public void testValidateDefaultPart() {
		assertTrue(sdv.verify("mail"));
		assertFalse(sdv.verify("mail/domain/f/face_bok"));
		assertFalse(sdv.verify("mail/domain/f/face.bok"));
	}

}
