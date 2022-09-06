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
package net.bluemind.domain.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.service.internal.DomainServerHook;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.GroupSearchQuery;
import net.bluemind.group.api.IGroup;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class DomainServerHookTests {
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		ElasticsearchTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		PopulateHelper.initGlobalVirt();
		IDomainSettings settings0 = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, "global.virt");
		Map<String, String> domainSettings0 = settings0.get();
		domainSettings0.put(DomainSettingsKeys.mail_routing_relay.name(), "external@test.fr");
		settings0.set(domainSettings0);
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.external);
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void testUserAndAdminGroupsHaveADatalocation() throws ServerFault, Exception {
		// create domain
		final String domainUid = "bluemind.test.net";
		PopulateHelper.addDomain(domainUid);

		// execute hook (as if a imap server is created)
		final ItemValue<Server> server = new ItemValue<>();
		server.uid = "bm";
		final ItemValue<Domain> domain = new ItemValue<>();
		domain.uid = domainUid;
		new DomainServerHook().onServerAssigned(
				ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext(), server, domain,
				"mail/imap");

		// user and admin groups should have a datalocation
		final String userGroupDatalocation = this.retrieveGroupDatalocation("user", domainUid);
		final String adminGroupDatalocation = this.retrieveGroupDatalocation("admin", domainUid);
		final boolean bothGroupsHaveADatalocation = userGroupDatalocation != null && !userGroupDatalocation.isEmpty()
				&& adminGroupDatalocation != null && !adminGroupDatalocation.isEmpty();
		Assert.assertTrue(String.format(
				"Both 'user' and 'admin' groups should have a datalocation. userGroupDatalocation=%s, adminGroupDatalocation=%s",
				userGroupDatalocation, adminGroupDatalocation), bothGroupsHaveADatalocation);

	}

	private String retrieveGroupDatalocation(final String groupName, final String domainUid) {
		final IGroup groupService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IGroup.class,
				domainUid);
		final GroupSearchQuery groupSearchQuery = GroupSearchQuery.matchProperty("is_profile", "true");
		groupSearchQuery.name = groupName;
		final List<ItemValue<Group>> groupResult = groupService.search(groupSearchQuery);
		Assert.assertEquals(1, groupResult.size());
		return groupResult.get(0).value.dataLocation;
	}

	@Test
	public void testDomainHookEmailAliasRemoved() throws Exception {
		final String domainUid = "bm.lan";
		IDomains domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		PopulateHelper.createDomain(domainUid, "bm.lan", "bm.fr");
		IUser userService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class,
				domainUid);
		User u = PopulateHelper.getUser("lolo", domainUid, Routing.internal);
		u.emails = Arrays.asList(Email.create("lolo@bm.lan", false), Email.create("lolo@bm.fr", true));
		userService.create("lolo", u);

		ItemValue<User> user = userService.byLogin("lolo");
		System.err.println("new aliases: " + user.value.emails);
		Assert.assertEquals(2, user.value.emails.size());

		// Now, remove the domain alias
		ItemValue<Domain> dom = domainService.get(domainUid);
		System.err.println("domain aliases: " + dom.value.aliases);

		domainService.setDefaultAlias(domainUid, "bm.fr");
		Set<String> aliases = Sets.newHashSet();
		aliases.add("bm.fr");
		TaskRef taskRef = domainService.setAliases(domainUid, aliases);
		TaskUtils.logStreamWait(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM), taskRef);

		dom = domainService.get(domainUid);
		System.err.println("domain aliases: " + dom.value.aliases);

		user = userService.byLogin("lolo");
		// user alias @domain.uid should be there
		Optional<Email> usermail = user.value.emails.stream().filter(e -> "bm.lan".equals(e.domainPart())).findAny();
		Assert.assertTrue(usermail.isPresent());

		System.err.println("user emails after DomainHook (hopefully): " + user.value.emails);
	}

}
