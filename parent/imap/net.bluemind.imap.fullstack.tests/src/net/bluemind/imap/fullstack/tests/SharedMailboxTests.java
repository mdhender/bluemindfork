/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.fullstack.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.columba.ristretto.message.Address;
import org.columba.ristretto.smtp.SMTPProtocol;
import org.columba.ristretto.smtp.SMTPResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;

import net.bluemind.addressbook.domainbook.verticle.DomainBookVerticle;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.imap.CreateMailboxResult;
import net.bluemind.imap.Flag;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPByteSource;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.NameSpaceInfo;
import net.bluemind.imap.SearchQuery;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.TaggedResult;
import net.bluemind.imap.driver.mailapi.DriverConfig;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.service.common.DefaultFolder;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;

public class SharedMailboxTests {

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
	}

	private String alias;
	private ItemValue<User> userShare;
	private ItemValue<Mailshare> mboxShare;

	@Before
	public void before() throws Exception {
		System.err.println("==== BEFORE starts ====");
		DomainBookVerticle.suspended = true;
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		Assert.assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo);
		String domUid = "devenv.blue";
		this.alias = "devenv.red";
		PopulateHelper.addDomain(domUid, Routing.internal, alias);

		ElasticsearchTestHelper.getInstance().beforeTest();

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		String userUid = PopulateHelper.addUser("john", "devenv.blue", Routing.internal);
		assertNotNull(userUid);

		this.userShare = sharedUser("jane", prov, domUid, userUid);

		this.mboxShare = sharedMailshare("ms", prov, domUid, userUid);

		StateContext.setInternalState(new RunningState());
		System.err.println("==== BEFORE ends ====");

	}

	@After
	public void after() throws Exception {
		System.err.println("===== AFTER starts =====");
		JdbcTestHelper.getInstance().afterTest();
		System.err.println("===== AFTER ends =====");
	}

	private ItemValue<User> sharedUser(String loginPrefix, ServerSideServiceProvider prov, String domUid,
			String userUid) {
		String janeUid = PopulateHelper.addUser(loginPrefix + System.currentTimeMillis(), "devenv.blue",
				Routing.internal);
		assertNotNull(janeUid);

		String janeAcls = IMailboxAclUids.uidForMailbox(janeUid);
		IContainerManagement mgmt = prov.instance(IContainerManagement.class, janeAcls);
		List<AccessControlEntry> curAcls = new ArrayList<>(mgmt.getAccessControlList());
		curAcls.add(AccessControlEntry.create(userUid, Verb.All));
		mgmt.setAccessControlList(curAcls);
		IUserSubscription subs = prov.instance(IUserSubscription.class, domUid);
		subs.subscribe(userUid, Collections.singletonList(ContainerSubscription.create(janeAcls, true)));
		IMailboxes mboxes = prov.instance(IMailboxes.class, domUid);
		ItemValue<Mailbox> asMbox = mboxes.getComplete(janeUid);
		asMbox.value.quota = 50000;
		mboxes.update(janeUid, asMbox.value);
		return prov.instance(IUser.class, domUid).getComplete(janeUid);
	}

	private ItemValue<Mailshare> sharedMailshare(String prefix, ServerSideServiceProvider prov, String domUid,
			String userUid) {
		String msName = prefix + System.currentTimeMillis();
		String msUid = UUID.randomUUID().toString();

		IMailshare ms = prov.instance(IMailshare.class, domUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = msName;
		mailshare.emails = Arrays.asList(Email.create(msName + "@" + alias, false, false),
				Email.create(msName + "@" + domUid, true, false));
		mailshare.routing = Routing.internal;
		mailshare.quota = 100000;
		ms.create(msUid, mailshare);

		IContainerManagement cmgmt = prov.instance(IContainerManagement.class, IMailboxAclUids.uidForMailbox(msUid));
		List<AccessControlEntry> accessControlList = new ArrayList<>(cmgmt.getAccessControlList());
		accessControlList.add(AccessControlEntry.create(userUid, Verb.All));
		cmgmt.setAccessControlList(accessControlList);

		IUserSubscription subs = prov.instance(IUserSubscription.class, domUid);
		subs.subscribe(userUid,
				Collections.singletonList(ContainerSubscription.create(IMailboxAclUids.uidForMailbox(msUid), true)));

		return ms.getComplete(msUid);
	}

	@Test
	public void listReturnsSharedItems() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			int noselect = 0;
			int otherUsers = 0;
			int mailshares = 0;
			ListResult results = sc.listAll();
			for (ListInfo li : results) {
				System.err.println(" - " + li);
				if (!li.isSelectable()) {
					noselect++;
				} else {
					if (li.getName().startsWith(DriverConfig.get().getString(DriverConfig.USER_VIRTUAL_ROOT) + "/")) {
						otherUsers++;
					} else if (li.getName()
							.startsWith(DriverConfig.get().getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/")) {
						mailshares++;
					}

					TaggedResult statusOk = sc.tagged(
							"STATUS \"" + UTF7Converter.encode(li.getName()) + "\" (UIDNEXT MESSAGES UNSEEN RECENT)");
					assertTrue("status on '" + li.getName() + "' failed: " + statusOk.getOutput(), statusOk.isOk());
					assertTrue("could not select " + li.getName(), sc.select(li.getName()));
				}
			}
			assertEquals(0, noselect);
			assertTrue("other users folders should appear in imap hierarchy", otherUsers > 0);
			assertTrue("mailshares should appear in imap hierarchy", mailshares > 0);
		}
	}

	@Test
	public void fetchQuotaRoots() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			ListResult results = sc.listAll();
			Set<String> roots = new HashSet<>();
			for (ListInfo li : results) {
				System.err.println(" - " + li);
				if (li.isSelectable()) {
					TaggedResult fetchQuota = sc.tagged("GETQUOTAROOT \"" + UTF7Converter.encode(li.getName()) + "\"");
					assertTrue(fetchQuota.isOk());
					Optional<String> optRoot = Arrays.stream(fetchQuota.getOutput())
							.filter(s -> s.startsWith("* QUOTAROOT ")).map(s -> {
								int betweenFolders = s.indexOf("\" \"");
								if (betweenFolders == -1) {
									return null;
								}
								return s.substring(betweenFolders + 3, s.length() - 1);
							}).findAny();
					assertTrue(optRoot.isPresent());
					boolean added = roots.add(optRoot.orElseThrow());
					if (added) {
						Arrays.stream(fetchQuota.getOutput()).filter(s -> s.contains("STORAGE"))
								.forEach(System.err::println);
					}
				}
			}
			assertEquals(3, roots.size());
		}
	}

	@Test
	public void listOnlySharedMbox() throws Exception {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String folder = DriverConfig.get().getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/" + mboxShare.value.name;
			TaggedResult subList = sc.tagged("LIST \"\" \"" + UTF7Converter.encode(folder) + "\"");
			assertTrue(subList.isOk());
			for (String s : subList.getOutput()) {
				System.err.println(s);
			}
			// root, sent, trash, ok completed.
			assertEquals(4, subList.getOutput().length);
		}
	}

	@Test
	public void deliveryToSharedMboxOverLmtp() throws Exception {
		String from = "wick" + System.currentTimeMillis() + "@continental.lan";
		try (SMTPProtocol smtp = new SMTPProtocol("127.0.0.1", 2400)) {
			smtp.openPort();
			smtp.mail(new Address("john@devenv.blue"));
			smtp.rcpt(new Address("+" + mboxShare.value.defaultEmailAddress()));
			SMTPResponse response = smtp.data(dumbEml(from));
			assertEquals(250, response.getCode());
		}
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String sharedFolder = DriverConfig.get().getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/"
					+ mboxShare.value.name;
			assertTrue(sc.select(sharedFolder));
			Collection<Integer> existing = sc.uidSearch(new SearchQuery());
			assertEquals(1, existing.size());
			try (IMAPByteSource eml = sc.uidFetchMessage(existing.iterator().next())) {
				String reRead = eml.source().asCharSource(StandardCharsets.US_ASCII).read();
				assertTrue(reRead.contains(from));
			}
			// copy it to my mailbox
			Map<Integer, Integer> fromShareToUser = sc.uidCopy(existing, "INBOX");
			System.err.println("copy result: " + fromShareToUser);
			assertFalse(fromShareToUser.isEmpty());

			// and the other way around
			sc.select("INBOX");
			Map<Integer, Integer> fromUserToShare = sc.uidCopy(fromShareToUser.values(), sharedFolder);
			assertFalse(fromUserToShare.isEmpty());
			sc.select(sharedFolder);
			Collection<Integer> afterCopy = sc.uidSearch(new SearchQuery());
			assertEquals(existing.size() + 1, afterCopy.size());
		}
	}

	@Test
	public void deliveryToSharedUserOverLmtp() throws Exception {
		String from = "wick" + System.currentTimeMillis() + "@continental.lan";
		try (SMTPProtocol smtp = new SMTPProtocol("127.0.0.1", 2400)) {
			smtp.openPort();
			smtp.mail(new Address("john@devenv.blue"));
			smtp.rcpt(new Address(userShare.value.defaultEmailAddress()));
			SMTPResponse response = smtp.data(dumbEml(from));
			assertEquals(250, response.getCode());
		}
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			assertTrue(sc.select(
					DriverConfig.get().getString(DriverConfig.USER_VIRTUAL_ROOT) + "/" + userShare.value.login));
			Collection<Integer> existing = sc.uidSearch(new SearchQuery());
			assertEquals(1, existing.size());
			try (IMAPByteSource eml = sc.uidFetchMessage(existing.iterator().next())) {
				String reRead = eml.source().asCharSource(StandardCharsets.US_ASCII).read();
				assertTrue(reRead.contains(from));
			}
		}

	}

	private InputStream dumbEml(String from) {
		return new ByteArrayInputStream(("From: " + from + "\r\n").getBytes());
	}

	@Test
	public void imapAppendToSharedMbox() throws Exception {
		String from = "wick" + System.currentTimeMillis() + "@continental.lan";

		FlagsList emptyFlags = new FlagsList();

		FlagsList seenFlag = new FlagsList();
		seenFlag.add(Flag.SEEN);

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, -1);
		Date inThePast = cal.getTime();

		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String folder = DriverConfig.get().getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/" + mboxShare.value.name;
			int folderAndEml = sc.append(folder, dumbEml(from), emptyFlags);
			assertEml(sc, folder, from, folderAndEml);

			int folderEmlAndSeen = sc.append(folder, dumbEml(from), seenFlag);
			assertEml(sc, folder, from, folderEmlAndSeen);

			int folderEmlSeenAndDate = sc.append(folder, dumbEml(from), seenFlag, inThePast);
			assertEml(sc, folder, from, folderEmlSeenAndDate);

		}
	}

	private void assertEml(StoreClient sc, String folder, String inEmlText, int imapUid)
			throws IMAPException, IOException {
		assertTrue("imap uid returned by append must be > 0 when successful", imapUid > 0);
		assertTrue(sc.select(folder));
		try (IMAPByteSource eml = sc.uidFetchMessage(imapUid)) {
			assertNotNull(eml);
			String reRead = eml.source().asCharSource(StandardCharsets.US_ASCII).read();
			assertTrue(reRead.contains(inEmlText));
		}

	}

	@Test
	public void createInSharedMbox() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String folder1 = DriverConfig.get().getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/" + mboxShare.value.name
					+ "/la.fille";
			assertTrue(sc.create(folder1));

			String folder2 = folder1 + "/du.bedouin";
			assertTrue(sc.create(folder2));

		}
	}

	@Test
	public void createInSharedUser() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			String root = DriverConfig.get().getString(DriverConfig.USER_VIRTUAL_ROOT) + "/" + userShare.value.login;

			String folder1 = root + "/la.fille";
			assertTrue(sc.create(folder1));

			String folder2 = folder1 + "/du.bedouin";
			assertTrue(sc.create(folder2));

			String folder3 = root + "/INBOX/sous.in.box";
			assertTrue(sc.create(folder3));

			String folder4 = folder3 + "/deeper";
			assertTrue(sc.create(folder4));

			for (ListInfo li : sc.listAll()) {
				System.err.println(li.getName() + (li.isSelectable() ? "(s)" : ""));
			}
			Set<String> foldersFromThisTest = Set.of(folder1, folder2, folder3, folder4);
			long inTheList = sc.listAll().stream().filter(ListInfo::isSelectable).map(ListInfo::getName)
					.filter(foldersFromThisTest::contains).count();
			assertEquals("One of the fresh folders is not listed ", foldersFromThisTest.size(), inTheList);

			CreateMailboxResult delRes = sc.deleteMailbox(folder4);
			assertTrue("del folder4 failed", delRes.isOk());

			delRes = sc.deleteMailbox(folder3);
			assertTrue("del folder3 failed", delRes.isOk());

			delRes = sc.deleteMailbox(folder2);
			assertTrue("del folder2 failed", delRes.isOk());

			delRes = sc.deleteMailbox(folder1);
			assertTrue("del folder1 failed", delRes.isOk());

			boolean listsDeletedFolder = sc.listAll().stream().filter(ListInfo::isSelectable).map(ListInfo::getName)
					.anyMatch(foldersFromThisTest::contains);
			assertFalse("One of " + foldersFromThisTest + " is still present", listsDeletedFolder);
		}
	}

	@Test
	public void renameToSharedMbox() throws IMAPException {
		Config config = DriverConfig.get();
		String mboxSharePrefix = config.getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/" + mboxShare.value.name;
		String userSharePrefix = config.getString(DriverConfig.USER_VIRTUAL_ROOT) + "/" + userShare.value.login;
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			assertTrue(sc.create("created"));
			assertTrue(sc.rename("created", "moved"));
			assertTrue(sc.rename("moved", "Trash/moved"));
			assertFalse(sc.rename("Trash/moved", mboxSharePrefix + "/moved"));
			assertFalse(sc.rename("Trash/moved", userSharePrefix + "/moved"));

			ListResult results = sc.listAll();
			results.stream().anyMatch(result -> result.getName().equals("Trash/moved"));
			results.stream().noneMatch(result -> result.getName().equals(mboxSharePrefix + "/moved"));
			results.stream().noneMatch(result -> result.getName().equals(userSharePrefix + "/moved"));

			assertTrue(sc.create(userSharePrefix + "/created"));
			assertTrue(sc.rename(userSharePrefix + "/created", userSharePrefix + "/moved"));
			assertTrue(sc.rename(userSharePrefix + "/moved", userSharePrefix + "/Trash/moved"));
			assertTrue(sc.rename(userSharePrefix + "/Trash/moved", userSharePrefix + "/INBOX/moved"));
			results = sc.listAll();
			results.stream().anyMatch(result -> result.getName().equals(userSharePrefix + "/INBOX/moved"));

			assertTrue(sc.create(mboxSharePrefix + "/created"));
			assertFalse(sc.rename(mboxSharePrefix + "/created", userSharePrefix + "/created"));
			results.stream().anyMatch(result -> result.getName().equals(mboxSharePrefix + "/created"));
			results.stream().noneMatch(result -> result.getName().equals(userSharePrefix + "/created"));

		}
	}

	@Test
	public void namespaceCmd() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			NameSpaceInfo nsi = sc.namespace();
			System.err.println("nsi pers: " + nsi.getPersonal());
			System.err.println("nsi othe: " + nsi.getOtherUsers());
			System.err.println("nsi shar: " + nsi.getMailShares());

		}
	}

	@Test
	public void ensureWeCantDeleteImportantFolder() throws IMAPException {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, "john@devenv.blue", "john")) {
			assertTrue(sc.login());
			Set<String> deletedFolders = new HashSet<>();

			sc.create("INBOX/notDel");
			sc.create("INBOX/notDel/Tutu");

			String folder1 = DriverConfig.get().getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/" + mboxShare.value.name
					+ "/deletable";
			sc.create(folder1);

			String folder2 = DriverConfig.get().getString(DriverConfig.USER_VIRTUAL_ROOT) + "/" + userShare.value.login
					+ "/deletable";
			sc.create(folder2);

			for (ListInfo li : sc.listAll()) {
				if (!li.isSelectable()) {
					continue;
				}
				CreateMailboxResult deleted = sc.deleteMailbox(li.getName());
				if (deleted.isOk()) {
					System.err.println("We deleted " + li.getName());
					deletedFolders.add(li.getName());
				}
			}

			assertFalse("INBOX must not be deletable over imap", deletedFolders.contains("INBOX"));
			DefaultFolder.USER_FOLDERS_NAME
					.forEach(f -> assertFalse(f + " must not be deletable", deletedFolders.contains(f)));
			System.err.println("We did " + deletedFolders.size() + " deletions: " + deletedFolders);
			assertEquals(3, deletedFolders.size());
		}
	}

}
