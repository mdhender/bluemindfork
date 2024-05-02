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
package net.bluemind.imap.vt.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.state.RunningState;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;

public abstract class BaseClientTests {

	@BeforeClass
	public static void sysprop() {
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
	}

	protected String domUid;

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		Assert.assertNotNull(esServer.ip);
		esServer.tags = List.of(TagDescriptor.bm_es.getTag());

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo, esServer);
		this.domUid = "devenv.blue";
		PopulateHelper.addDomain(domUid, Routing.internal);

		ElasticsearchTestHelper.getInstance().beforeTest();

		addUser("john", domUid);

		StateContext.setInternalState(new RunningState());
		System.err.println("==== BEFORE ====");

	}

	protected String addUser(String login, String domUid) {
		String addedUid = PopulateHelper.addUser(login, domUid, Routing.internal);
		assertNotNull(addedUid);

		IMailboxes mboxes = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class,
				domUid);
		ItemValue<Mailbox> asMbox = mboxes.getComplete(addedUid);
		asMbox.value.quota = 50000;
		mboxes.update(addedUid, asMbox.value);
		return addedUid;
	}

	@After
	public void after() throws Exception {
		System.err.println("===== AFTER =====");
		JdbcTestHelper.getInstance().afterTest();
	}

}
