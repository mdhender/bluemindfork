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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import net.bluemind.addressbook.domainbook.verticle.DomainBookVerticle;
import net.bluemind.backend.mail.replica.service.tests.AbstractRollingReplicationTests.ImapActions;
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
import net.bluemind.core.sessions.Sessions;
import net.bluemind.imap.StoreClient;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailshare.api.IMailshare;
import net.bluemind.mailshare.api.Mailshare;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserSubscription;
import net.bluemind.user.api.User;

public class MailApiTestsBase {

	private ServerSideServiceProvider serverProv;

	protected String domUid;
	protected String alias;
	protected String userUid;

	protected ServerSideServiceProvider userProvider;

	@Rule
	public TestName testName = new TestName();

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@BeforeClass
	public static void afterClass() {
		System.clearProperty("node.local.ipaddr");
		System.clearProperty("imap.local.ipaddr");
		System.clearProperty("ahcnode.fail.https.ok");
	}

	@Before
	public void before() throws Exception {
		System.err.println("==== BEFORE " + testName.getMethodName() + " starts ====");
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

		PopulateHelper.initGlobalVirt(pipo, esServer);

		Topology.get().nodes().forEach(ivs -> {
			System.err.println(" * " + ivs);
		});

		// SplittedShardsMapping.map(new BmConfIni().get("imap-role"),
		// PopulateHelper.FAKE_CYRUS_IP);

		long time = System.currentTimeMillis();
		this.domUid = "devenv" + time + ".blue";
		this.alias = "devenv" + time + ".red";
		PopulateHelper.addDomain(domUid, Routing.internal, alias);

		ElasticsearchTestHelper.getInstance().beforeTest();

		this.serverProv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		this.userUid = PopulateHelper.addUser("john", domUid, Routing.internal);
		assertNotNull(userUid);

		SecurityContext userSec = new SecurityContext("sid", userUid, Collections.emptyList(), Collections.emptyList(),
				domUid);
		Sessions.get().put("sid", userSec);
		this.userProvider = ServerSideServiceProvider.getProvider(userSec);

		StateContext.setInternalState(new RunningState());
		System.err.println("==== BEFORE " + testName.getMethodName() + " ends ====");

	}

	@After
	public void after() throws Exception {
		System.err.println("===== AFTER " + testName.getMethodName() + " starts =====");
		JdbcTestHelper.getInstance().afterTest();
		System.err.println("===== AFTER ends =====");
	}

	protected ItemValue<User> sharedUser(String loginPrefix, String domUid, String userUid) {
		String janeUid = PopulateHelper.addUser(loginPrefix + System.currentTimeMillis(), "devenv.blue",
				Routing.internal);
		assertNotNull(janeUid);

		String janeAcls = IMailboxAclUids.uidForMailbox(janeUid);
		IContainerManagement mgmt = serverProv.instance(IContainerManagement.class, janeAcls);
		List<AccessControlEntry> curAcls = new ArrayList<>(mgmt.getAccessControlList());
		curAcls.add(AccessControlEntry.create(userUid, Verb.All));
		mgmt.setAccessControlList(curAcls);
		IUserSubscription subs = serverProv.instance(IUserSubscription.class, domUid);
		subs.subscribe(userUid, Collections.singletonList(ContainerSubscription.create(janeAcls, true)));
		IMailboxes mboxes = serverProv.instance(IMailboxes.class, domUid);
		ItemValue<Mailbox> asMbox = mboxes.getComplete(janeUid);
		asMbox.value.quota = 50000;
		mboxes.update(janeUid, asMbox.value);
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
