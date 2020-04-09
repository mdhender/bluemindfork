package net.bluemind.system.ldap.export.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.junit.Test;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.system.ldap.export.services.LdapExportService;
import net.bluemind.system.ldap.export.services.PasswordUpdateService;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.ChangePassword;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class PasswordUpdateServiceTests extends LdapExportTests {
	@Test
	public void builder() throws Exception {
		try {
			PasswordUpdateService.build(null, null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			PasswordUpdateService.build("domainuid", null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			PasswordUpdateService.build("", null);
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		try {
			PasswordUpdateService.build("domainuid", "");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.INVALID_PARAMETER, sf.getCode());
		}

		assertFalse(PasswordUpdateService.build("invalidDomainUid", "useruid").isPresent());

		try {
			PasswordUpdateService.build(domain.uid, "invalidUserUid");
			fail("Test must thrown an exception");
		} catch (ServerFault sf) {
			assertEquals(ErrorCode.UNKNOWN, sf.getCode());
		}

		String userUid = PopulateHelper.addUser("test" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);

		assertTrue(PasswordUpdateService.build(domain.uid, userUid).isPresent());

		String noLdapExportDomainUid = "test" + System.currentTimeMillis() + ".lan";
		PopulateHelper.createTestDomain(noLdapExportDomainUid);
		assertFalse(LdapExportService.build(noLdapExportDomainUid).isPresent());
	}

	@Test
	public void noPreviousLastChangeNoLifetime() throws Exception {
		User user = PopulateHelper.getUser("test-" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		user.password = null;
		String userUid = PopulateHelper.addUser(domain.value.name, user);

		LdapExportService.build(domain.uid).get().sync();

		Entry entry = getUserEntry(userUid);
		assertFalse(entry.containsAttribute("shadowLastChange"));
		assertFalse(entry.containsAttribute("shadowMax"));

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain.uid)
				.setPassword(userUid, ChangePassword.create("testpassword"));

		PasswordUpdateService.build(domain.uid, userUid).get().sync();

		entry = getUserEntry(userUid);
		assertEquals(getTodayLastChangeValue(), entry.get("shadowLastChange").get().toString());
		assertFalse(entry.containsAttribute("shadowMax"));
	}

	@Test
	public void noPreviousLastChangeLifetime() throws Exception {
		User user = PopulateHelper.getUser("test-" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		user.password = null;
		String userUid = PopulateHelper.addUser(domain.value.name, user);

		LdapExportService.build(domain.uid).get().sync();

		Entry entry = getUserEntry(userUid);
		assertFalse(entry.containsAttribute("shadowLastChange"));
		assertFalse(entry.containsAttribute("shadowMax"));

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		Map<String, String> domainSettings = provider.instance(IDomainSettings.class, domain.uid).get();
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		provider.instance(IDomainSettings.class, domain.uid).set(domainSettings);

		provider.instance(IUser.class, domain.uid).setPassword(userUid, ChangePassword.create("testpassword"));

		PasswordUpdateService.build(domain.uid, userUid).get().sync();

		entry = getUserEntry(userUid);
		assertEquals(getTodayLastChangeValue(), entry.get("shadowLastChange").get().toString());
		assertEquals("10", entry.get("shadowMax").get().toString());
	}

	@Test
	public void noPreviousLastChangeLifetimeRemoved() throws Exception {
		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		Map<String, String> domainSettings = provider.instance(IDomainSettings.class, domain.uid).get();
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		provider.instance(IDomainSettings.class, domain.uid).set(domainSettings);

		User user = PopulateHelper.getUser("test-" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		user.password = null;
		String userUid = PopulateHelper.addUser(domain.value.name, user);

		LdapExportService.build(domain.uid).get().sync();

		Entry entry = getUserEntry(userUid);
		assertFalse(entry.containsAttribute("shadowLastChange"));
		assertEquals("10", entry.get("shadowMax").get().toString());

		domainSettings.remove(DomainSettingsKeys.password_lifetime.name());
		provider.instance(IDomainSettings.class, domain.uid).set(domainSettings);

		provider.instance(IUser.class, domain.uid).setPassword(userUid, ChangePassword.create("testpassword"));

		PasswordUpdateService.build(domain.uid, userUid).get().sync();

		entry = getUserEntry(userUid);
		assertEquals(getTodayLastChangeValue(), entry.get("shadowLastChange").get().toString());
		assertFalse(entry.containsAttribute("shadowMax"));
	}

	@Test
	public void passwordNeverExpire() throws Exception {
		User user = PopulateHelper.getUser("test-" + System.nanoTime(), domain.value.name, Mailbox.Routing.none);
		user.password = null;
		user.passwordNeverExpires = true;
		String userUid = PopulateHelper.addUser(domain.value.name, user);

		LdapExportService.build(domain.uid).get().sync();

		Entry entry = getUserEntry(userUid);
		assertFalse(entry.containsAttribute("shadowLastChange"));
		assertFalse(entry.containsAttribute("shadowMax"));

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		Map<String, String> domainSettings = provider.instance(IDomainSettings.class, domain.uid).get();
		domainSettings.put(DomainSettingsKeys.password_lifetime.name(), "10");
		provider.instance(IDomainSettings.class, domain.uid).set(domainSettings);

		provider.instance(IUser.class, domain.uid).setPassword(userUid, ChangePassword.create("testpassword"));

		PasswordUpdateService.build(domain.uid, userUid).get().sync();

		entry = getUserEntry(userUid);
		assertEquals(getTodayLastChangeValue(), entry.get("shadowLastChange").get().toString());
		assertFalse(entry.containsAttribute("shadowMax"));
	}

	private String getTodayLastChangeValue() {
		return Long.toString(new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay());
	}
}
