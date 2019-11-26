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
package net.bluemind.domain.validator.tests;

import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;

import com.google.common.collect.Lists;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.service.internal.DomainSettingsValidator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.persistence.MailboxStore;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainSettingsValidatorTests {

	private DomainSettingsValidator validator;
	private String domainUid;
	private ContainerStoreService<Mailbox> mailboxStoreService;

	@Before
	public void before() throws Exception {

		domainUid = "bm.lan";

		JdbcTestHelper.getInstance().beforeTest();
		
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.createTestDomain(domainUid, esServer);

		ContainerStore containerStore = new ContainerStore(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		Container mailboxContainer = containerStore.get(domainUid);

		MailboxStore mailboxStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), mailboxContainer);
		mailboxStoreService = new ContainerStoreService<>(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM, mailboxContainer, "mailbox", mailboxStore);

		validator = new DomainSettingsValidator();

		final CountDownLatch launched = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				launched.countDown();
			}
		});
		launched.await();

	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void splitDomainValidatorTests() {
		try {
			validator.create(Collections.<String, String> emptyMap());
		} catch (ServerFault e) {
			fail();
		}

		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");

		try {
			validator.create(settings);
			fail();
		} catch (ServerFault e) {
		}

		settings.put(DomainSettingsKeys.mail_routing_relay.name(), "");
		try {
			validator.create(settings);
			fail();
		} catch (ServerFault e) {
		}

		settings.put(DomainSettingsKeys.mail_routing_relay.name(), "       ");
		try {
			validator.create(settings);
			fail();
		} catch (ServerFault e) {
		}

		settings.put(DomainSettingsKeys.mail_routing_relay.name(), "whatever");
		try {
			validator.create(settings);
		} catch (ServerFault e) {
			fail();
		}
	}

	@Test
	public void splitDomainExternalMailboxValidator() throws ServerFault {

		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "false");

		Map<String, String> oldSettings = new HashMap<>();
		oldSettings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");
		oldSettings.put(DomainSettingsKeys.mail_routing_relay.name(), "whatever.bm.lan");
		try {
			validator.update(oldSettings, settings, "bm.lan");
		} catch (ServerFault sf) {
			fail();
		}

		// external user
		Mailbox mailbox = defaultMailbox("splitDomainExternalUserValidator." + System.nanoTime());
		mailboxStoreService.create(UUID.randomUUID().toString(), mailbox.name, mailbox);
		try {
			validator.update(oldSettings, settings, "bm.lan");
			fail();
		} catch (ServerFault sf) {
		}
	}

	private Mailbox defaultMailbox(String name) {
		Mailbox mailbox = new Mailbox();
		mailbox.type = Type.user;
		mailbox.name = name;
		mailbox.routing = Routing.external;
		mailbox.archived = false;
		mailbox.hidden = false;
		mailbox.routing = Routing.external;

		return mailbox;
	}

}
