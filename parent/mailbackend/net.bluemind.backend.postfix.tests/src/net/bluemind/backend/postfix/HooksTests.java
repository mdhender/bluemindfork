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

package net.bluemind.backend.postfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.persistence.DirEntryStore;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.persistence.DomainStore;
import net.bluemind.domain.service.DomainsContainerIdentifier;
import net.bluemind.group.api.Group;
import net.bluemind.group.persistence.GroupStore;
import net.bluemind.group.service.internal.ContainerGroupStoreService;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.service.internal.MailboxStoreService;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.service.internal.ContainerUserStoreService;

public abstract class HooksTests {
	protected static final String DOMAIN_UID = "bm.lan";

	private ContainerStore containerHome;
	protected String mailSmtpTestIp;
	protected Container domainContainer;
	protected ItemStore groupItemStore;
	protected GroupStore groupStore;
	protected ContainerGroupStoreService groupStoreService;
	protected ContainerUserStoreService userStoreService;
	protected ItemStore userItemStore;
	protected ItemValue<Server> dataLocationServer;
	protected Container domainsContainer;
	protected BmTestContext bmContext;

	protected MailboxStoreService mailboxStore;
	protected Domain testDomain;

	protected ItemValue<Domain> domain;

	protected List<String> mapsFileNames = Arrays.asList("/etc/postfix/virtual_domains", "/etc/postfix/virtual_mailbox",
			"/etc/postfix/virtual_alias", "/etc/postfix/transport", "/etc/postfix/master_relay_transport");

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		containerHome = new ContainerStore(new BmTestContext(SecurityContext.SYSTEM),
				JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM);

		domainsContainer = Container.create(DomainsContainerIdentifier.getIdentifier(), "domain", "domain", "me");
		domainsContainer = containerHome.create(domainsContainer);

		createHosts();

		Domain domainItem = domainItem(DOMAIN_UID);
		domain = PopulateHelper.createTestDomain(DOMAIN_UID, domainItem);
		Map<String, String> domainSettings = domainSettings();
		PopulateHelper.createDomainSettings(DOMAIN_UID, domainSettings);
		domainContainer = containerHome.get(DOMAIN_UID);

		initUser(domain);
		initGroup(domain);
		ItemValue<Domain> domainValue = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).get(DOMAIN_UID);

		domain = domainValue;
		testDomain = domainValue.value;

		assignHosts(DOMAIN_UID);

		Container mboxContainer = containerHome.get(DOMAIN_UID);

		mailboxStore = new MailboxStoreService(JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM,
				mboxContainer);

		bmContext = new BmTestContext(SecurityContext.SYSTEM);

		final SettableFuture<Void> future = SettableFuture.<Void>create();
		Handler<AsyncResult<Void>> done = new Handler<AsyncResult<Void>>() {

			@Override
			public void handle(AsyncResult<Void> event) {
				future.set(null);
			}
		};
		VertxPlatform.spawnVerticles(done);
		future.get();

		Activator.DISABLE_EVENT = true;
	}

	protected abstract String getTestTag();

	protected abstract String getServerIp();

	private void createHosts() throws Exception {
		// register elasticsearch to locator
		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		// register TEST_TAG host to locator
		mailSmtpTestIp = getServerIp();
		Server smtpServer = new Server();
		smtpServer.ip = mailSmtpTestIp;
		smtpServer.tags = Lists.newArrayList(getTestTag());

		// DataLocation server
		Server dataLocation = new Server();
		dataLocation.ip = "10.0.0.1";
		dataLocation.tags = new ArrayList<String>();

		PopulateHelper.initGlobalVirt(esServer, smtpServer, dataLocation);

		dataLocationServer = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(dataLocation.ip);
	}

	private void assignHosts(String domainUid) throws Exception, ServerFault, IOException {
		PopulateHelper.assign(JdbcTestHelper.getInstance().getDataSource(), mailSmtpTestIp, getTestTag(), domainUid);

		List<Assignment> assignments = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getAssignments(domainUid);
		assertEquals(1, assignments.stream().filter(assign -> assign.serverUid.equals(mailSmtpTestIp)).count());
	}

	protected Domain domainItem(String domainUid) throws ServerFault {
		return Domain.create(domainUid, domainUid, domainUid, Collections.emptySet());
	}

	protected Map<String, String> domainSettings() {
		return Collections.<String, String>emptyMap();
	}

	private void initGroup(ItemValue<Domain> domain) throws SQLException {

		groupStore = new GroupStore(JdbcTestHelper.getInstance().getDataSource(), domainContainer);
		groupStoreService = new ContainerGroupStoreService(new BmTestContext(SecurityContext.SYSTEM), domainContainer,
				domain);
		groupItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), domainContainer,
				SecurityContext.SYSTEM);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	private void initUser(ItemValue<Domain> domain) throws SQLException, Exception {

		userStoreService = new ContainerUserStoreService(new BmTestContext(SecurityContext.SYSTEM), domainContainer,
				domain);

		userItemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), domainContainer,
				SecurityContext.SYSTEM);
	}

	protected Mailbox defaultMailbox(Routing routing, boolean withAlias) {
		Mailbox mbox = new Mailbox();
		mbox.type = Type.user;
		mbox.name = "mailbox-" + System.nanoTime();
		mbox.routing = routing;
		if (mbox.routing == Routing.internal) {
			mbox.dataLocation = dataLocationServer.uid;
		}
		mbox.hidden = false;
		mbox.system = false;

		mbox.emails = new ArrayList<Email>(1);
		Email em = new Email();
		em.address = mbox.name + "@" + DOMAIN_UID;
		em.isDefault = true;
		em.allAliases = false;
		mbox.emails.add(em);

		if (withAlias) {
			em = new Email();
			em.address = UUID.randomUUID().toString() + "@" + DOMAIN_UID;
			em.isDefault = true;
			em.allAliases = false;
			mbox.emails.add(em);
		}

		return mbox;
	}

	protected ItemValue<Mailbox> createMailbox(Mailbox mbox) throws ServerFault, SQLException {
		String mailboxUid = UUID.randomUUID().toString();
		mailboxStore.create(mailboxUid, null, mbox);

		Container container = containerHome.get(DOMAIN_UID);
		ItemStore itemStore = new ItemStore(JdbcTestHelper.getInstance().getDataSource(), container,
				SecurityContext.SYSTEM);
		Item i = itemStore.get(mailboxUid);
		DirEntryStore dirEntryStore = new DirEntryStore(JdbcTestHelper.getInstance().getDataSource(), container);
		dirEntryStore.create(i,
				DirEntry.create(null, i.uid, DirEntry.Kind.MAILSHARE, i.uid, "domain",
						mbox.defaultEmail() != null ? mbox.defaultEmail().address : "", mbox.hidden, mbox.system,
						mbox.archived, mbox.dataLocation));

		return ItemValue.create(mailboxUid, mbox);
	}

	protected void createMailbox(String uid, Mailbox mbox) throws ServerFault {

		mailboxStore.attach(uid, null, mbox);
	}

	protected void compareMapContent(String mapPath, HashMap<String, Set<String>> expectedContent)
			throws IOException, ServerFault {
		compareMapContent(mailSmtpTestIp, mapPath, expectedContent);
	}

	protected void compareMapContent(String serverIp, String mapPath, HashMap<String, Set<String>> expectedContent)
			throws IOException, ServerFault {
		String mapContent = new String(NodeActivator.get(serverIp).read(mapPath));
		System.out.println(mapPath + ": " + mapContent);

		BufferedReader br = new BufferedReader(new StringReader(mapContent));
		String mapLine = null;
		while ((mapLine = br.readLine()) != null) {
			if (mapLine.trim().isEmpty()) {
				continue;
			}

			int firstSpace = mapLine.indexOf(" ");
			if (firstSpace == -1 || firstSpace + 1 == mapLine.length()) {
				fail("Invalid map line: " + mapLine + ", no space character found");
			}

			String email = mapLine.substring(0, firstSpace);
			if (email.isEmpty() || !expectedContent.containsKey(email)) {
				fail("Invalid map line: " + mapLine + ", email is " + email + " expected were ["
						+ expectedContent.keySet() + "]");
			}

			String[] currentRecipients = mapLine.substring(firstSpace + 1).split(",");
			HashSet<String> recipients = new HashSet<String>(currentRecipients.length);
			for (String recipient : currentRecipients) {
				recipients.add(recipient);
			}

			assertEquals(0, Sets.symmetricDifference(expectedContent.get(email), recipients).size());
			expectedContent.remove(email);
		}

		assertEquals(0, expectedContent.size());
	}

	protected void rmMaps(String[] nodeIps) throws ServerFault {
		for (String nodeIp : nodeIps) {
			INodeClient nc = NodeActivator.get(nodeIp);

			for (String mapFileName : mapsFileNames) {
				nc.executeCommandNoOut("rm -f " + mapFileName + " " + mapFileName + "-flat " + mapFileName + ".db");
			}
		}
	}

	protected ItemValue<Group> createTestGroup(boolean withEmail) throws ServerFault {
		Group group = defaultGroup();
		if (!withEmail) {
			group.emails = new ArrayList<Email>();
		}

		String uid = UUID.randomUUID().toString();
		groupStoreService.create(uid, group);

		return ItemValue.create(uid, group);
	}

	protected Group defaultGroup() {
		Group group = new Group();

		group.name = "group-" + System.nanoTime();
		group.description = "Test group";

		Email e = new Email();
		e.address = group.name + "@" + DOMAIN_UID;
		e.allAliases = true;
		e.isDefault = true;
		group.emails = new ArrayList<Email>(1);
		group.emails.add(e);

		return group;
	}

	protected void updateTestDomain(Domain domain) throws ServerFault {
		DomainStore domainStore = new DomainStore(JdbcTestHelper.getInstance().getDataSource());
		ContainerStoreService<Domain> domainStoreService = new ContainerStoreService<Domain>(
				JdbcTestHelper.getInstance().getDataSource(), SecurityContext.SYSTEM, domainsContainer, domainStore);

		domainStoreService.update(domain.name, domain.name, domain);
		CacheRegistry.get().invalidateAll();
	}

	protected void updateTestDomainSettings(String domainUid, String key, String value) throws ServerFault {
		IDomainSettings service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid);
		Map<String, String> settings = service.get();
		settings.put(key, value);
		service.set(settings);
	}
}
