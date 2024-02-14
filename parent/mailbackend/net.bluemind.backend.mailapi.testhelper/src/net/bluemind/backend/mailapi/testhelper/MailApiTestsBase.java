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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mailapi.testhelper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.google.common.base.Throwables;
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
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.directory.hollow.datamodel.consumer.DirectorySearchFactory;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.driver.mailapi.DriverConfig;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.network.topology.Topology;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;
import net.bluemind.utils.ByteSizeUnit;

public class MailApiTestsBase {

	@FunctionalInterface
	public static interface ImapActions<T> {

		T run(StoreClient sc) throws Exception;
	}

	protected IServiceProvider serverProv;

	protected String domUid;
	protected String alias;
	public String userUid;

	protected IServiceProvider userProvider;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP + "," + PopulateHelper.FAKE_CYRUS_IP_2);
		System.setProperty("ahcnode.fail.https.ok", "true");
		System.setProperty("mapi.notification.fresh", "true");
	}

	@BeforeClass
	public static void afterClass() {
		System.clearProperty("node.local.ipaddr");
		System.clearProperty("imap.local.ipaddr");
		System.clearProperty("ahcnode.fail.https.ok");
		System.clearProperty("mapi.notification.fresh");
	}

	protected String imapRoot(ItemValue<Mailshare> share) {
		Config config = DriverConfig.get();
		return config.getString(DriverConfig.SHARED_VIRTUAL_ROOT) + "/" + share.value.name;
	}

	protected boolean suspendBookSync() {
		return true;
	}

	protected boolean setupUserAndProvider() {
		return true;
	}

	protected void cleanupHollowData() {
		System.err.println("cleanup of hollow data");
		DirectorySearchFactory.reset();

		File dir = new File("/var/spool/bm-hollowed/directory");
		if (dir.exists() && dir.isDirectory()) {
			try {
				Files.walk(dir.toPath(), FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder())
						.map(Path::toFile).forEach(File::delete);
			} catch (Exception e) {
				e.printStackTrace();
			}
			dir.mkdirs();
		}
	}

	@Before
	public void before() throws Exception {
		System.err.println("==== BEFORE " + testName.getMethodName() + " starts ====");
		DomainBookVerticle.suspended = suspendBookSync();
		if (!suspendBookSync()) {
			cleanupHollowData();
		}
		JdbcTestHelper.getInstance().beforeTest();
		ElasticsearchTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		Assert.assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo, esServer);

		Topology.get().nodes().forEach(ivs -> {
			System.err.println(" * " + ivs);
		});

		long time = System.currentTimeMillis();
		this.domUid = "devenv" + time + ".blue";
		this.alias = "devenv" + time + ".red";
		PopulateHelper.addDomain(domUid, Routing.internal, alias);

		this.serverProv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		if (setupUserAndProvider()) {
			this.userUid = PopulateHelper.addUser("john", domUid, Routing.internal, BasicRoles.ROLE_OUTLOOK);
			assertNotNull(userUid);

			SecurityContext userSec = new SecurityContext("sid", userUid, Collections.emptyList(),
					Collections.emptyList(), domUid);
			Sessions.get().put("sid", userSec);
			this.userProvider = ServerSideServiceProvider.getProvider(userSec);
		}

		StateContext.setInternalState(new RunningState());
		System.err.println("==== BEFORE " + testName.getMethodName() + " ends ====");

	}

	@After
	public void after() throws Exception {
		System.err.println("===== AFTER " + testName.getMethodName() + " starts =====");
		ElasticsearchTestHelper.getInstance().afterTest();
		JdbcTestHelper.getInstance().afterTest();
		if (!suspendBookSync()) {
			cleanupHollowData();
		}
		System.err.println("===== AFTER ends =====");
	}

	protected ItemValue<User> sharedUser(String loginPrefix, String domUid, String userUid) {
		return sharedUser(loginPrefix, domUid, userUid, 50, ByteSizeUnit.MB);
	}

	protected ItemValue<User> sharedUser(String loginPrefix, String domUid, String userUid, Integer quota,
			ByteSizeUnit quotaUnit) {
		String janeUid = PopulateHelper.addUser(loginPrefix + System.currentTimeMillis(), domUid, Routing.internal,
				BasicRoles.ROLE_OUTLOOK);
		assertNotNull(janeUid);

		String janeAcls = IMailboxAclUids.uidForMailbox(janeUid);
		IContainerManagement mgmt = serverProv.instance(IContainerManagement.class, janeAcls);
		List<AccessControlEntry> curAcls = new ArrayList<>(mgmt.getAccessControlList());
		curAcls.add(AccessControlEntry.create(userUid, Verb.All));
		mgmt.setAccessControlList(curAcls);
		IUserSubscription subs = serverProv.instance(IUserSubscription.class, domUid);
		subs.subscribe(userUid, Collections.singletonList(ContainerSubscription.create(janeAcls, true)));
		IMailboxes mboxes = serverProv.instance(IMailboxes.class, domUid);
		if (quota != null) {
			ItemValue<Mailbox> asMbox = mboxes.getComplete(janeUid);
			asMbox.value.quota = (int) quotaUnit.toKB(quota);
			mboxes.update(janeUid, asMbox.value);
		}
		return serverProv.instance(IUser.class, domUid).getComplete(janeUid);
	}

	protected ItemValue<Mailshare> sharedMailshare(String prefix, String domUid, String userUid) {
		String msName = prefix + System.currentTimeMillis();
		String msUid = UUID.randomUUID().toString();

		IMailshare ms = serverProv.instance(IMailshare.class, domUid);
		Mailshare mailshare = new Mailshare();
		mailshare.name = msName;
		mailshare.emails = Arrays.asList(Email.create(msName + "@" + alias, false, false),
				Email.create(msName + "@" + domUid, true, false));
		mailshare.routing = Routing.internal;
		mailshare.quota = 100000;
		ms.create(msUid, mailshare);

		IContainerManagement cmgmt = serverProv.instance(IContainerManagement.class,
				IMailboxAclUids.uidForMailbox(msUid));
		List<AccessControlEntry> accessControlList = new ArrayList<>(cmgmt.getAccessControlList());
		accessControlList.add(AccessControlEntry.create(userUid, Verb.All));
		cmgmt.setAccessControlList(accessControlList);

		IUserSubscription subs = serverProv.instance(IUserSubscription.class, domUid);
		subs.subscribe(userUid,
				Collections.singletonList(ContainerSubscription.create(IMailboxAclUids.uidForMailbox(msUid), true)));

		return ms.getComplete(msUid);
	}

	public final <T> T imapAsUser(ImapActions<T> actions) {
		return imapAction(userUid + "@" + domUid, userUid, actions);
	}

	public final <T> T imapAsUser(String userUidForImap, ImapActions<T> actions) {
		return imapAction(userUidForImap + "@" + domUid, userUidForImap, actions);
	}

	private <T> T imapAction(String imapLogin, String imapPass, ImapActions<T> actions) {
		try (StoreClient sc = new StoreClient("127.0.0.1", 1143, imapLogin, imapPass)) {
			assertTrue(sc.login());
			return actions.run(sc);
		} catch (Exception e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}

}
