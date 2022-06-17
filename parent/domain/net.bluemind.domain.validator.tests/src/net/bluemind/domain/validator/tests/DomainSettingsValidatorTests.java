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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.service.internal.DomainSettingsValidator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.persistence.MailboxStore;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainSettingsValidatorTests {

	private DomainSettingsValidator validator;
	private String domainUid;
	private String otherDomainUid;
	private ContainerStoreService<Mailbox> mailboxStoreService;
	private BmTestContext admin0 = new BmTestContext(SecurityContext.SYSTEM);

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("ahcnode.fail.https.ok", "true");
	}

	@Before
	public void before() throws Exception {
		domainUid = "bm.lan";
		otherDomainUid = "otherdomain.lan";

		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(esServer);

		PopulateHelper.createTestDomain(domainUid, esServer);
		PopulateHelper.createTestDomain(otherDomainUid, esServer);

		ContainerStore containerStore = new ContainerStore(null, JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM);
		Container mailboxContainer = containerStore.get(domainUid);

		MailboxStore mailboxStore = new MailboxStore(JdbcTestHelper.getInstance().getDataSource(), mailboxContainer);
		mailboxStoreService = new ContainerStoreService<>(JdbcTestHelper.getInstance().getDataSource(),
				SecurityContext.SYSTEM, mailboxContainer, mailboxStore);

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
			validator.create(admin0, Collections.<String, String>emptyMap(), domainUid);
		} catch (ServerFault e) {
			fail();
		}

		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.mail_forward_unknown_to_relay.name(), "true");

		try {
			validator.create(admin0, settings, domainUid);
			fail();
		} catch (ServerFault e) {
		}

		settings.put(DomainSettingsKeys.mail_routing_relay.name(), "");
		try {
			validator.create(admin0, settings, domainUid);
			fail();
		} catch (ServerFault e) {
		}

		settings.put(DomainSettingsKeys.mail_routing_relay.name(), "       ");
		try {
			validator.create(admin0, settings, domainUid);
			fail();
		} catch (ServerFault e) {
		}

		settings.put(DomainSettingsKeys.mail_routing_relay.name(), "whatever");
		try {
			validator.create(admin0, settings, domainUid);
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
			validator.update(admin0, oldSettings, settings, "bm.lan");
		} catch (ServerFault sf) {
			fail();
		}

		// external user
		Mailbox mailbox = defaultMailbox("splitDomainExternalUserValidator." + System.nanoTime());
		mailboxStoreService.create(UUID.randomUUID().toString(), mailbox.name, mailbox);
		try {
			validator.update(admin0, oldSettings, settings, "bm.lan");
			fail();
		} catch (ServerFault sf) {
		}
	}

	@Test
	public void externalUrl_create() {
		initDomainsUrls();

		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		validator.create(admin0, settings, domainUid);

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "invalid-fqdn");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "url.global.tld");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "other2.url.global.tld");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "url.otherdomain.tld");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "other2.url.otherdomain.tld");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void externalUrl_update() {
		initDomainsUrls();

		Map<String, String> oldSettings = Collections.emptyMap();

		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		validator.update(admin0, oldSettings, settings, domainUid);

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "invalid-fqdn");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "url.global.tld");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "other2.url.global.tld");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "url.otherdomain.tld");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "other2.url.otherdomain.tld");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void otherUrl_create() {
		initDomainsUrls();

		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld other2.domain.tld");
		validator.create(admin0, settings, domainUid);

		// external-url not set
		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld other2.domain.tld");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld invalid-url");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld url.global.tld");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld other1.url.global.tld");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld url.otherdomain.tld");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld other2.url.otherdomain.tld");
		try {
			validator.create(admin0, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	@Test
	public void otherUrl_update() {
		initDomainsUrls();

		Map<String, String> oldSettings = Collections.emptyMap();

		Map<String, String> settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld other2.domain.tld");
		validator.update(admin0, oldSettings, settings, domainUid);

		// external-url not set
		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld other2.domain.tld");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld invalid-url");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld url.global.tld");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld other1.url.global.tld");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld url.otherdomain.tld");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		settings = new HashMap<>();
		settings.put(DomainSettingsKeys.external_url.name(), "valid.domain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.domain.tld other2.url.otherdomain.tld");
		try {
			validator.update(admin0, oldSettings, settings, domainUid);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}
	}

	private void initDomainsUrls() {
		HashMap<String, String> globalSettings = new HashMap<>();
		globalSettings.put(SysConfKeys.external_url.name(), "url.global.tld");
		globalSettings.put(SysConfKeys.other_urls.name(), "other1.url.global.tld other2.url.global.tld");
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(ISystemConfiguration.class)
				.updateMutableValues(globalSettings);

		Map<String, String> settings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, otherDomainUid).get();
		settings.put(DomainSettingsKeys.external_url.name(), "url.otherdomain.tld");
		settings.put(DomainSettingsKeys.other_urls.name(), "other1.url.otherdomain.tld other2.url.otherdomain.tld");
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, otherDomainUid)
				.set(settings);
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
