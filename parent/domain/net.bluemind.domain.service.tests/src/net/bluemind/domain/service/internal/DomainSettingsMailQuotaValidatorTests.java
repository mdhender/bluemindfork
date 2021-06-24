package net.bluemind.domain.service.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomains;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DomainSettingsMailQuotaValidatorTests {
	private ItemValue<Domain> domain;
	private BmContext adminContext = new BmTestContext(SecurityContext.SYSTEM);

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		String domainUid = "bm.lan";

		PopulateHelper.initGlobalVirt();

		PopulateHelper.createTestDomain(domainUid);
		domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class).get(domainUid);
	}

	@After
	public void after() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void checkOk() {
		new DomainSettingsMailQuotaValidator(adminContext)
				.create(new DomainSettings(domain.uid, Collections.emptyMap()));

		new DomainSettingsMailQuotaValidator(adminContext).create(new DomainSettings(domain.uid, getDomainSettings()));

		new DomainSettingsMailQuotaValidator(adminContext).update(
				new DomainSettings(domain.uid, Collections.emptyMap()),
				new DomainSettings(domain.uid, Collections.emptyMap()));

		new DomainSettingsMailQuotaValidator(adminContext).update(new DomainSettings(domain.uid, getDomainSettings()),
				new DomainSettings(domain.uid, getDomainSettings()));

		new DomainSettingsMailQuotaValidator(adminContext).update(
				new DomainSettings(domain.uid, getDomainSettings("50", "10")),
				new DomainSettings(domain.uid, getDomainSettings("40", "20")));
	}

	@Test
	public void create_defaultGreaterThanMax() {
		Map<String, String> domainSettings = getDomainSettings();
		domainSettings.put(DomainSettingsKeys.mailbox_default_user_quota.name(), "100");

		try {
			new DomainSettingsMailQuotaValidator(adminContext).create(new DomainSettings(domain.uid, domainSettings));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Default user quota is greater than quota max"));
		}

		domainSettings = getDomainSettings();
		domainSettings.put(DomainSettingsKeys.mailbox_default_publicfolder_quota.name(), "100");

		try {
			new DomainSettingsMailQuotaValidator(adminContext).create(new DomainSettings(domain.uid, domainSettings));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Default mailshare quota is greater than quota max"));
		}
	}

	@Test
	public void update_defaultGreaterThanMax() {
		Map<String, String> domainSettings = getDomainSettings();
		domainSettings.put(DomainSettingsKeys.mailbox_default_user_quota.name(), "100");

		try {
			new DomainSettingsMailQuotaValidator(adminContext).update(
					new DomainSettings(domain.uid, Collections.emptyMap()),
					new DomainSettings(domain.uid, domainSettings));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Default user quota is greater than quota max"));
		}

		domainSettings = getDomainSettings();
		domainSettings.put(DomainSettingsKeys.mailbox_default_publicfolder_quota.name(), "100");

		try {
			new DomainSettingsMailQuotaValidator(adminContext).update(
					new DomainSettings(domain.uid, Collections.emptyMap()),
					new DomainSettings(domain.uid, domainSettings));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("Default mailshare quota is greater than quota max"));
		}
	}

	@Test
	public void update_maxLesserThanAssigned() {
		Mailbox mailbox = new Mailbox();
		mailbox.name = "test" + System.currentTimeMillis();
		mailbox.type = Type.mailshare;
		mailbox.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		mailbox.quota = 200;
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, domain.uid)
				.create(UUID.randomUUID().toString(), mailbox);

		try {
			new DomainSettingsMailQuotaValidator(adminContext).update(
					new DomainSettings(domain.uid, Collections.emptyMap()),
					new DomainSettings(domain.uid, getDomainSettings()));
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
			assertTrue(sf.getMessage().startsWith("At least one mailbox quota is greater than"));
		}
	}

	@Test
	public void update_noQuotaMaxWithMailboxQuotaAssigned() {
		Mailbox mailbox = new Mailbox();
		mailbox.name = "test" + System.currentTimeMillis();
		mailbox.type = Type.user;
		mailbox.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		mailbox.quota = 200;
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, domain.uid)
				.create(UUID.randomUUID().toString(), mailbox);

		mailbox = new Mailbox();
		mailbox.name = "test" + System.currentTimeMillis();
		mailbox.type = Type.mailshare;
		mailbox.dataLocation = PopulateHelper.FAKE_CYRUS_IP;
		mailbox.quota = 200;
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, domain.uid)
				.create(UUID.randomUUID().toString(), mailbox);

		new DomainSettingsMailQuotaValidator(adminContext).update(
				new DomainSettings(domain.uid, getDomainSettings("10", "10")),
				new DomainSettings(domain.uid, getDomainSettings("0", "10")));
	}

	private Map<String, String> getDomainSettings() {
		return getDomainSettings("50", "10");
	}

	private Map<String, String> getDomainSettings(String quotaMax, String quotaDefault) {
		Map<String, String> domainSettings = new HashMap<>();
		domainSettings.put(DomainSettingsKeys.mailbox_max_user_quota.name(), quotaMax);
		domainSettings.put(DomainSettingsKeys.mailbox_default_user_quota.name(), quotaDefault);

		domainSettings.put(DomainSettingsKeys.mailbox_max_publicfolder_quota.name(), quotaMax);
		domainSettings.put(DomainSettingsKeys.mailbox_default_publicfolder_quota.name(), quotaDefault);

		return domainSettings;
	}
}
