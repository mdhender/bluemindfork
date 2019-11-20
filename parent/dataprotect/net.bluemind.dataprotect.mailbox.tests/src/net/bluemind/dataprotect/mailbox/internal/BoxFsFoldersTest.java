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
package net.bluemind.dataprotect.mailbox.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;

public class BoxFsFoldersTest {

	@Test
	public void testSimpleUserPath() {
		Domain domainValue = new Domain();
		domainValue.name = "test.loc";
		ItemValue<Domain> domain = new ItemValue<>();
		domain.uid = "test.loc";
		domain.value = domainValue;

		Mailbox mboxValue = new Mailbox();
		mboxValue.name = "john.doe";
		mboxValue.type = Type.user;
		mboxValue.dataLocation = "location";
		ItemValue<Mailbox> mbox = new ItemValue<>();
		mbox.value = mboxValue;

		DataProtectGeneration dpg = new DataProtectGeneration();
		dpg.id = 1;
		dpg.protectionTime = new Date();

		BoxFsFolders bff = BoxFsFolders.build(domain, mbox, dpg);

		Set<String> folders = bff.allFolders();
		assertEquals(
				new HashSet<>(Arrays.asList(
						"/var/spool/cyrus/data/location__test_loc/domain/t/test.loc/j/user/john^doe",
						"/var/spool/cyrus/meta/location__test_loc/domain/t/test.loc/j/user/john^doe",
						"/var/spool/bm-hsm/cyrus-archives/location__test_loc/domain/t/test.loc/j/user/john^doe")),
				folders);

		assertEquals(
				new HashSet<>(
						Arrays.asList("/var/spool/cyrus/data/location__test_loc/domain/t/test.loc/j/user/john^doe")),
				bff.dataPath);
		assertEquals(
				new HashSet<>(
						Arrays.asList("/var/spool/cyrus/meta/location__test_loc/domain/t/test.loc/j/user/john^doe")),
				bff.metaPath);

		assertEquals(
				new HashSet<>(Arrays.asList(
						"/var/spool/bm-hsm/cyrus-archives/location__test_loc/domain/t/test.loc/j/user/john^doe")),
				bff.archivePath);

		assertEquals("restored-" + (new SimpleDateFormat("yyyy-MM-dd")).format(dpg.protectionTime),
				bff.restoreFolderName);

		assertEquals("/var/spool/cyrus/data/location__test_loc/domain/t/test.loc/j/user/john^doe/restored-"
				+ (new SimpleDateFormat("yyyy-MM-dd")).format(dpg.protectionTime), bff.restoreDataRoot);
		assertEquals("/var/spool/cyrus/meta/location__test_loc/domain/t/test.loc/j/user/john^doe/restored-"
				+ (new SimpleDateFormat("yyyy-MM-dd")).format(dpg.protectionTime), bff.restoreMetaRoot);
	}

	@Test
	public void testSpecialUserPath() {
		Domain domainValue = new Domain();
		domainValue.name = "test.loc";
		ItemValue<Domain> domain = new ItemValue<>();
		domain.uid = "test.loc";
		domain.value = domainValue;

		Mailbox mboxValue = new Mailbox();
		mboxValue.name = "0815john.doe";
		mboxValue.type = Type.user;
		mboxValue.dataLocation = "location";
		ItemValue<Mailbox> mbox = new ItemValue<>();
		mbox.value = mboxValue;

		DataProtectGeneration dpg = new DataProtectGeneration();
		dpg.id = 1;
		dpg.protectionTime = new Date();

		Set<String> folders = BoxFsFolders.build(domain, mbox, dpg).allFolders();

		assertEquals(
				new HashSet<>(Arrays.asList(
						"/var/spool/cyrus/data/location__test_loc/domain/t/test.loc/q/user/0815john^doe",
						"/var/spool/cyrus/meta/location__test_loc/domain/t/test.loc/q/user/0815john^doe",
						"/var/spool/bm-hsm/cyrus-archives/location__test_loc/domain/t/test.loc/q/user/0815john^doe")),
				folders);

	}

	@Test
	public void testSpecialDomainPath() {
		Domain domainValue = new Domain();
		domainValue.name = "0815test.loc";
		ItemValue<Domain> domain = new ItemValue<>();
		domain.uid = "0815test.loc";
		domain.value = domainValue;

		Mailbox mboxValue = new Mailbox();
		mboxValue.name = "john.doe";
		mboxValue.type = Type.user;
		mboxValue.dataLocation = "location";
		ItemValue<Mailbox> mbox = new ItemValue<>();
		mbox.value = mboxValue;

		DataProtectGeneration dpg = new DataProtectGeneration();
		dpg.id = 1;
		dpg.protectionTime = new Date();

		Set<String> folders = BoxFsFolders.build(domain, mbox, dpg).allFolders();

		assertEquals(new HashSet<>(Arrays.asList(
				"/var/spool/cyrus/meta/location__0815test_loc/domain/q/0815test.loc/j/user/john^doe",
				"/var/spool/cyrus/data/location__0815test_loc/domain/q/0815test.loc/j/user/john^doe",
				"/var/spool/bm-hsm/cyrus-archives/location__0815test_loc/domain/q/0815test.loc/j/user/john^doe")),
				folders);

	}

	@Test
	public void testSpecialUserAndDomainPath() {
		Domain domainValue = new Domain();
		domainValue.name = "0815test.loc";
		ItemValue<Domain> domain = new ItemValue<>();
		domain.uid = "0815test.loc";
		domain.value = domainValue;

		Mailbox mboxValue = new Mailbox();
		mboxValue.name = "%john.doe";
		mboxValue.type = Type.user;
		mboxValue.dataLocation = "location";
		ItemValue<Mailbox> mbox = new ItemValue<>();
		mbox.value = mboxValue;

		DataProtectGeneration dpg = new DataProtectGeneration();
		dpg.id = 1;
		dpg.protectionTime = new Date();

		Set<String> folders = BoxFsFolders.build(domain, mbox, dpg).allFolders();

		assertEquals(new HashSet<>(Arrays.asList(
				"/var/spool/cyrus/data/location__0815test_loc/domain/q/0815test.loc/q/user/%john^doe",
				"/var/spool/cyrus/meta/location__0815test_loc/domain/q/0815test.loc/q/user/%john^doe",
				"/var/spool/bm-hsm/cyrus-archives/location__0815test_loc/domain/q/0815test.loc/q/user/%john^doe")),
				folders);

	}

	@Test
	public void testSimpleMailsharePath() {
		Domain domainValue = new Domain();
		domainValue.name = "test.loc";
		ItemValue<Domain> domain = new ItemValue<>();
		domain.uid = "test.loc";
		domain.value = domainValue;

		Mailbox mboxValue = new Mailbox();
		mboxValue.name = "mymailshare";
		mboxValue.type = Type.mailshare;
		mboxValue.dataLocation = "location";
		ItemValue<Mailbox> mbox = new ItemValue<>();
		mbox.value = mboxValue;

		DataProtectGeneration dpg = new DataProtectGeneration();
		dpg.id = 1;
		dpg.protectionTime = new Date();

		BoxFsFolders bff = BoxFsFolders.build(domain, mbox, dpg);

		char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();

		Set<String> folders = bff.allFolders();
		for (char c : chars) {
			assertTrue(folders
					.contains("/var/spool/cyrus/data/location__test_loc/domain/t/test.loc/" + c + "/mymailshare"));
			assertTrue(folders
					.contains("/var/spool/cyrus/meta/location__test_loc/domain/t/test.loc/" + c + "/mymailshare"));
		}

		folders = bff.dataPath;
		for (char c : chars) {
			assertTrue(folders
					.contains("/var/spool/cyrus/data/location__test_loc/domain/t/test.loc/" + c + "/mymailshare"));
		}

		folders = bff.metaPath;
		for (char c : chars) {
			assertTrue(folders
					.contains("/var/spool/cyrus/meta/location__test_loc/domain/t/test.loc/" + c + "/mymailshare"));
		}

		assertEquals("restored-" + (new SimpleDateFormat("yyyy-MM-dd")).format(dpg.protectionTime),
				bff.restoreFolderName);

		assertEquals("/var/spool/cyrus/data/location__test_loc/domain/t/test.loc/r/mymailshare/restored-"
				+ (new SimpleDateFormat("yyyy-MM-dd")).format(dpg.protectionTime), bff.restoreDataRoot);
		assertEquals("/var/spool/cyrus/meta/location__test_loc/domain/t/test.loc/r/mymailshare/restored-"
				+ (new SimpleDateFormat("yyyy-MM-dd")).format(dpg.protectionTime), bff.restoreMetaRoot);
	}

	@Test
	public void testSpecialMailsharePath() {
		Domain domainValue = new Domain();
		domainValue.name = "0815test.loc";
		ItemValue<Domain> domain = new ItemValue<>();
		domain.uid = "0815test.loc";
		domain.value = domainValue;

		Mailbox mboxValue = new Mailbox();
		mboxValue.name = "%mymailshare";
		mboxValue.type = Type.mailshare;
		mboxValue.dataLocation = "location";
		ItemValue<Mailbox> mbox = new ItemValue<>();
		mbox.value = mboxValue;

		DataProtectGeneration dpg = new DataProtectGeneration();
		dpg.id = 1;
		dpg.protectionTime = new Date();

		Set<String> folders = BoxFsFolders.build(domain, mbox, dpg).allFolders();
		folders.forEach(System.out::println);

		char[] chars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
		for (char c : chars) {
			assertTrue(folders.contains(
					"/var/spool/cyrus/data/location__0815test_loc/domain/q/0815test.loc/" + c + "/%mymailshare"));
			assertTrue(folders.contains(
					"/var/spool/cyrus/meta/location__0815test_loc/domain/q/0815test.loc/" + c + "/%mymailshare"));
		}

	}
}
