/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.mailbox.service.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainFilterHookTests {

	protected String domainUid;

	@Before
	public void before() throws Exception {
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		domainUid = "bm.lan";

		Server pipo = new Server();
		pipo.tags = Collections.singletonList(TagDescriptor.mail_imap.getTag());
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;

		PopulateHelper.initGlobalVirt(pipo);

		PopulateHelper.createTestDomain(domainUid, pipo);
	}

	@Test
	public void testDomainDeletionShouldDeleteAllMailfilters() {
		IMailboxes domainMailboxesService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IMailboxes.class, domainUid);

		MailFilter filter = new MailFilter();
		MailFilterRule rule = new MailFilterRule();
		rule.conditions.add(MailFilterRuleCondition.equal("from", "blub"));
		rule.addDiscard();
		rule.active = false;
		filter.rules = Arrays.asList(rule);
		domainMailboxesService.setDomainFilter(filter);

		assertEquals(1, domainMailboxesService.getDomainFilter().rules.size());

		// would fail if filter persists
		IDomains service = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		TaskRef deleteDomainItems = service.deleteDomainItems(domainUid);
		waitFor(deleteDomainItems);
		service.delete(domainUid);
	}

	private void waitFor(TaskRef taskRef) throws ServerFault {
		ITask task = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ITask.class, taskRef.id);
		while (!task.status().state.ended) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}

		TaskStatus status = task.status();
		if (status.state == State.InError) {
			throw new ServerFault("error");
		}
	}

}
