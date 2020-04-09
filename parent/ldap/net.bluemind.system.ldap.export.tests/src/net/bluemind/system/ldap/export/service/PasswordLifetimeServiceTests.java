package net.bluemind.system.ldap.export.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.UUID;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.system.ldap.export.services.LdapExportService;
import net.bluemind.system.ldap.export.services.PasswordLifetimeService;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class PasswordLifetimeServiceTests extends LdapExportTests {
	@Test
	public void builder() throws Exception {
		try {
			PasswordLifetimeService.build(null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			PasswordLifetimeService.build("");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		assertFalse(PasswordLifetimeService.build("invalidUid").isPresent());
		assertTrue(LdapExportService.build(domain.uid).isPresent());

		String noLdapExportDomainUid = "test" + System.currentTimeMillis() + ".lan";
		PopulateHelper.createTestDomain(noLdapExportDomainUid);
		assertFalse(LdapExportService.build(noLdapExportDomainUid).isPresent());
	}

	@Test
	public void lifetimeNotSet() throws Exception {
		String userUid = PopulateHelper.addUser(UUID.randomUUID().toString(), domain.value.name);

		LdapExportService.build(domain.uid).get().sync();

		Entry entry = getUserEntry(userUid);
		assertNull(entry.get("shadowMax"));

		PasswordLifetimeService.build(domain.uid).get().sync();

		entry = getUserEntry(userUid);
		assertNull(entry.get("shadowMax"));
	}

	@Test
	public void lifetimeSet() throws Exception {
		IDomainSettings domainSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domain.uid);
		Map<String, String> domainSettings = domainSettingsService.get();
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		domainSettingsService.set(domainSettings);

		String userUid = PopulateHelper.addUser(UUID.randomUUID().toString(), domain.value.name);

		LdapExportService.build(domain.uid).get().sync();

		Entry entry = getUserEntry(userUid);
		assertEquals("10", entry.get("shadowMax").get().toString());

		PasswordLifetimeService.build(domain.uid).get().sync();

		entry = getUserEntry(userUid);
		assertEquals("10", entry.get("shadowMax").get().toString());
	}

	@Test
	public void lifetimeUpdate() throws Exception {
		String userUid = PopulateHelper.addUser(UUID.randomUUID().toString(), domain.value.name);

		LdapExportService.build(domain.uid).get().sync();

		Entry entry = getUserEntry(userUid);
		assertNull(entry.get("shadowMax"));

		PasswordLifetimeService.build(domain.uid).get().sync();

		entry = getUserEntry(userUid);
		assertNull(entry.get("shadowMax"));

		IDomainSettings domainSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domain.uid);
		Map<String, String> domainSettings = domainSettingsService.get();
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		domainSettingsService.set(domainSettings);

		PasswordLifetimeService.build(domain.uid).get().sync();

		entry = getUserEntry(userUid);
		assertEquals("10", entry.get("shadowMax").get().toString());

		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "20");
		domainSettingsService.set(domainSettings);

		PasswordLifetimeService.build(domain.uid).get().sync();

		entry = getUserEntry(userUid);
		assertEquals("20", entry.get("shadowMax").get().toString());

		domainSettings.remove(DomainSettingsKeys.password_lifetime.name());
		domainSettingsService.set(domainSettings);

		PasswordLifetimeService.build(domain.uid).get().sync();

		entry = getUserEntry(userUid);
		assertNull(entry.get("shadowMax"));
	}

	@Test
	public void lifetimeNeverExpireUser() throws Exception {
		String userUid = PopulateHelper.addUser(UUID.randomUUID().toString(), domain.value.name);
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domain.uid).getComplete(userUid);
		user.value.passwordNeverExpires = true;
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain.uid).update(user.uid,
				user.value);

		LdapExportService.build(domain.uid).get().sync();
		PasswordLifetimeService.build(domain.uid).get().sync();

		Entry entry = getUserEntry(userUid);
		assertNull(entry.get("shadowMax"));

		IDomainSettings domainSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domain.uid);
		Map<String, String> domainSettings = domainSettingsService.get();
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		domainSettingsService.set(domainSettings);

		PasswordLifetimeService.build(domain.uid).get().sync();

		entry = getUserEntry(userUid);
		assertNull(entry.get("shadowMax"));
	}

	@Test
	public void lifetimeMustChangeUser() throws Exception {
		String userUid = PopulateHelper.addUser(UUID.randomUUID().toString(), domain.value.name);
		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, domain.uid).getComplete(userUid);
		user.value.passwordMustChange = true;
		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain.uid).update(user.uid,
				user.value);

		LdapExportService.build(domain.uid).get().sync();
		PasswordLifetimeService.build(domain.uid).get().sync();

		Entry entry = getUserEntry(userUid);
		assertEquals("0", entry.get("shadowMax").get().toString());

		IDomainSettings domainSettingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domain.uid);
		Map<String, String> domainSettings = domainSettingsService.get();
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		domainSettingsService.set(domainSettings);

		PasswordLifetimeService.build(domain.uid).get().sync();

		entry = getUserEntry(userUid);
		assertEquals("0", entry.get("shadowMax").get().toString());

		domainSettings.remove(DomainSettingsKeys.password_lifetime.name());
		domainSettingsService.set(domainSettings);

		PasswordLifetimeService.build(domain.uid).get().sync();

		entry = getUserEntry(userUid);
		assertEquals("0", entry.get("shadowMax").get().toString());
	}
}
