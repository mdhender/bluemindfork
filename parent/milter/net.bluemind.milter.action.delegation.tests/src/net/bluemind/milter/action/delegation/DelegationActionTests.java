/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.milter.action.delegation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.stream.Field;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.mailflow.rbe.CoreClientContext;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.IMilterListener;
import net.bluemind.milter.SmtpAddress;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.milter.cache.DomainAliasCache;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.api.IUserSettings;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserMailIdentity;

public class DelegationActionTests {

	private IClientContext cliContext;
	private ItemValue<Mailbox> mailboxFrom;
	private ItemValue<Mailbox> mailboxSendAs;
	private ItemValue<Mailbox> mailboxSendOnBehalf;
	private ItemValue<Mailbox> mailboxWrite;
	private ItemValue<Mailbox> mailboxRead;
	private ItemValue<Mailbox> mailboxAll;

	private String domainUid;

	private static final String DOMAIN_ALIAS = "test.bm.lan";
	private static final String DOMAIN_ALIAS_1 = "test1.bm.lan";

	private static final String IDENTITY_ID = "test_identity";

	public static class DomainAliasCacheFiller extends DomainAliasCache {
		public static void addDomain(ItemValue<Domain> domain) {
			domain.value.aliases.forEach(alias -> {
				domainCache.put(alias, domain);
			});
		}
	}

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		Server imapServer = new Server();
		imapServer.ip = PopulateHelper.FAKE_CYRUS_IP;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(PopulateHelper.FAKE_CYRUS_IP);

		ItemValue<Domain> domainTestItem = PopulateHelper.createTestDomain("tester.internal");
		domainTestItem.value.aliases.add("tester.internal");
		DomainAliasCacheFiller.addDomain(domainTestItem);

		ItemValue<Domain> domainItem = PopulateHelper.createTestDomain("test.internal");
		domainItem.value.aliases.add("test.internal");
		domainItem.value.aliases.add(DOMAIN_ALIAS);
		domainItem.value.aliases.add(DOMAIN_ALIAS_1);
		DomainAliasCacheFiller.addDomain(domainItem);

		cliContext = new CoreClientContext(domainTestItem);
		domainUid = cliContext.getSenderDomain().uid;

		assertNotNull(cliContext.getSenderDomain().value.defaultAlias);

		PopulateHelper.addUser("hpot", domainUid);
		PopulateHelper.addUser("dumbledore", domainUid);
		PopulateHelper.addUser("mcgonagal", domainUid);
		PopulateHelper.addUserWithRoles("malefoy", domainUid, BasicRoles.ROLE_EXTERNAL_IDENTITY);
		PopulateHelper.addUser("rogue", domainUid);
		PopulateHelper.addUser("voldemort", domainUid);

		mailboxFrom = getServiceMailbox(domainUid).byEmail("hpot@" + domainUid);
		mailboxFrom.value.emails.add(Email.create("hpot@" + DOMAIN_ALIAS, false));
		mailboxFrom.value.emails.add(Email.create("h-pot@" + DOMAIN_ALIAS, false));
		mailboxFrom.value.emails.add(Email.create("harry@" + DOMAIN_ALIAS_1, false));
		getServiceMailbox(domainUid).update(mailboxFrom.uid, mailboxFrom.value);

		mailboxSendAs = getServiceMailbox(domainUid).byEmail("dumbledore@" + domainUid);
		mailboxSendAs.value.emails.add(Email.create("dumbledore@" + DOMAIN_ALIAS, false));
		mailboxSendAs.value.emails.add(Email.create("dumbledore@" + DOMAIN_ALIAS_1, false));
		getServiceMailbox(domainUid).update(mailboxSendAs.uid, mailboxSendAs.value);

		mailboxSendOnBehalf = getServiceMailbox(domainUid).byEmail("mcgonagal@" + domainUid);
		mailboxSendOnBehalf.value.emails.add(Email.create("mcgonagal@" + DOMAIN_ALIAS, false));
		mailboxSendOnBehalf.value.emails.add(Email.create("mcgonagal@" + DOMAIN_ALIAS_1, false));
		getServiceMailbox(domainUid).update(mailboxSendOnBehalf.uid, mailboxSendOnBehalf.value);

		mailboxWrite = getServiceMailbox(domainUid).byEmail("rogue@" + domainUid);
		mailboxWrite.value.emails.add(Email.create("rogue@" + DOMAIN_ALIAS, false));
		mailboxWrite.value.emails.add(Email.create("rogue@" + DOMAIN_ALIAS_1, false));
		getServiceMailbox(domainUid).update(mailboxWrite.uid, mailboxWrite.value);

		mailboxRead = getServiceMailbox(domainUid).byEmail("malefoy@" + domainUid);
		mailboxRead.value.emails.add(Email.create("malefoy@" + DOMAIN_ALIAS, false));
		mailboxRead.value.emails.add(Email.create("malefoy@" + DOMAIN_ALIAS_1, false));
		getServiceMailbox(domainUid).update(mailboxRead.uid, mailboxRead.value);

		mailboxAll = getServiceMailbox(domainUid).byEmail("voldemort@" + domainUid);
		mailboxAll.value.emails.add(Email.create("voldemort@" + DOMAIN_ALIAS, false));
		mailboxAll.value.emails.add(Email.create("voldemort@" + DOMAIN_ALIAS_1, false));
		getServiceMailbox(domainUid).update(mailboxAll.uid, mailboxAll.value);

		addUserSettings("malefoy");
		getServiceIdentity(domainUid, "malefoy").create(IDENTITY_ID, defaultIdentity());
		long identities = getServiceIdentity(domainUid, "malefoy").getIdentities().stream().count();
		assertEquals(2, identities);
	}

	private void addUserSettings(String userUid) throws ServerFault {
		HashMap<String, String> userSettings = new HashMap<>();
		userSettings.put("lang", "en");

		getSettingsService().set(userUid, userSettings);

		Map<String, String> us = getSettingsService().get(userUid);
		assertNotNull(us);
		assertTrue(us.size() > 0);
		assertEquals("18", us.get("work_hours_end"));
		assertEquals("en", us.get("lang"));
	}

	private UserMailIdentity defaultIdentity() {
		UserMailIdentity i = new UserMailIdentity();
		i.displayname = "hagrid";
		i.name = "hagrid";
		i.email = "hagrid-external@ext.test.lan";
		i.format = SignatureFormat.PLAIN;
		i.signature = "-- gg";
		i.sentFolder = "Sent";
		i.isDefault = false;
		return i;
	}

	protected IUserSettings getSettingsService() throws ServerFault {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUserSettings.class, domainUid);
	}

	private IUser getServiceUser(String domainUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid);
	}

	private IUserMailIdentities getServiceIdentity(String domainUid, String userUid) {
		// Test external identity
		String sid = "sid" + System.currentTimeMillis();
		SecurityContext context = new SecurityContext(sid, "admin@" + domainUid, new ArrayList<String>(),
				Arrays.asList(SecurityContext.ROLE_ADMIN, BasicRoles.ROLE_EXTERNAL_IDENTITY), domainUid);

		return ServerSideServiceProvider.getProvider(context).instance(IUserMailIdentities.class, domainUid, userUid);
	}

	private IContainerManagement getServiceManagement(String userContainer) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IContainerManagement.class,
				userContainer);
	}

	private IMailboxes getServiceMailbox(String domainUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, domainUid);
	}

	@Test
	public void testWithSenderAs() throws Exception {
		getServiceManagement(IMailboxAclUids.uidForMailbox("hpot"))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create("dumbledore", Verb.SendAs)));

		String senderAddress = mailboxSendAs.value.defaultEmail().address;
		UpdatedMailMessage mm = loadTemplate("sendAs.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(mailboxFrom.value.defaultEmail().address);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, Verb.SendAs);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithSenderAs_internal() throws Exception {
		getServiceManagement(IMailboxAclUids.uidForMailbox("hpot"))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create("dumbledore", Verb.SendAs)));

		String senderAddress = "dumbledore@" + domainUid; // mailboxSendAs
		UpdatedMailMessage mm = loadTemplate("sendAs.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(mailboxFrom.value.defaultEmail().address);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, Verb.SendAs);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithAll() throws Exception {
		getServiceManagement(IMailboxAclUids.uidForMailbox("hpot"))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create("voldemort", Verb.All)));

		String senderAddress = mailboxAll.value.defaultEmail().address;
		UpdatedMailMessage mm = loadTemplate("sendAs.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(mailboxFrom.value.defaultEmail().address);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, Verb.All);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithSenderOnBehalf() throws Exception {
		getServiceManagement(IMailboxAclUids.uidForMailbox("hpot"))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create("mcgonagal", Verb.SendOnBehalf)));

		String senderAddress = mailboxSendOnBehalf.value.emails.stream()
				.filter(e -> e.domainPart().equals(DOMAIN_ALIAS_1)).map(e -> e.address).findFirst().get();
		UpdatedMailMessage mm = loadTemplate("sendAs.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(mailboxFrom.value.defaultEmail().address);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, Verb.SendOnBehalf);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithSenderOnBehalf_internal() throws Exception {
		getServiceManagement(IMailboxAclUids.uidForMailbox("hpot"))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create("mcgonagal", Verb.SendOnBehalf)));

		String senderAddress = "mcgonagal@" + domainUid; // mailboxSendOnBehalf
		UpdatedMailMessage mm = loadTemplate("sendAs.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(mailboxFrom.value.defaultEmail().address);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		System.err.println("auth_authen => " + mm.properties.get("{auth_authen}"));

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, Verb.SendOnBehalf);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithWrite() throws Exception {
		getServiceManagement(IMailboxAclUids.uidForMailbox("hpot"))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create("rogue", Verb.Write)));

		String senderAddress = mailboxWrite.value.defaultEmail().address;
		UpdatedMailMessage mm = loadTemplate("sendAs.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(mailboxFrom.value.defaultEmail().address);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, Verb.Write);
		assertTrue(smtpError(mm));
	}

	@Test
	public void testWithRead() throws Exception {
		getServiceManagement(IMailboxAclUids.uidForMailbox("hpot"))
				.setAccessControlList(Arrays.asList(AccessControlEntry.create("malefoy", Verb.Read)));

		String senderAddress = mailboxRead.value.defaultEmail().address;
		UpdatedMailMessage mm = loadTemplate("sendAs.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(mailboxFrom.value.defaultEmail().address);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, Verb.Read);
		assertTrue(smtpError(mm));
	}

	@Test
	public void testWithMySelf_sameAlias() throws Exception {
		String senderAddress = "hpot@" + domainUid;
		UpdatedMailMessage mm = loadTemplate("sendMe_sameAlias.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress("hpot@" + DOMAIN_ALIAS);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, null);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithMySelf_differentAlias_domainuid() throws Exception {
		String senderAddress = "hpot@" + domainUid;
		UpdatedMailMessage mm = loadTemplate("sendMe_otherAlias.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress("h-pot@" + DOMAIN_ALIAS);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, null);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithMySelf_differentAlias() throws Exception {
		String senderAddress = "hpot@" + DOMAIN_ALIAS;
		UpdatedMailMessage mm = loadTemplate("sendMe_otherAlias.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress("h-pot@" + DOMAIN_ALIAS);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, null);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithIdentity_withRoleWithoutIdentity() throws IOException {
		getServiceIdentity(domainUid, mailboxRead.uid).delete(IDENTITY_ID);

		String senderAddress = mailboxRead.value.defaultEmail().address;
		UpdatedMailMessage mm = loadTemplate("sendAsExternal.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessageHeaders(mm, sender, Verb.Read);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithIdentity_withRoleWithIdentity() throws IOException {
		String senderAddress = mailboxRead.value.defaultEmail().address;
		UpdatedMailMessage mm = loadTemplate("sendAsExternal.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessageHeaders(mm, sender, Verb.Read);
		assertFalse(smtpError(mm));
	}

	@Test
	public void testWithIdentity_withoutRoleWithIdentity() throws IOException {
		getServiceUser(domainUid).setRoles("malefoy", new HashSet<String>());

		String senderAddress = mailboxRead.value.defaultEmail().address;
		UpdatedMailMessage mm = loadTemplate("sendAsExternal.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessageHeaders(mm, sender, Verb.Read);
		assertTrue(smtpError(mm));
	}

	@Test
	public void testWithIdentity_withoutRoleWithoutIdentity() throws IOException {
		getServiceUser(domainUid).setRoles("malefoy", new HashSet<String>());
		getServiceIdentity(domainUid, mailboxRead.uid).delete(IDENTITY_ID);

		String senderAddress = mailboxRead.value.defaultEmail().address;
		UpdatedMailMessage mm = loadTemplate("sendAsExternal.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessageHeaders(mm, sender, Verb.Read);
		assertTrue(smtpError(mm));
	}

	private UpdatedMailMessage loadTemplate(String name, String sender) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		InputStream is = this.getClass().getResourceAsStream("/templates/" + name);

		int nRead;
		byte[] data = new byte[1024];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		Map<String, Collection<String>> propMap = new HashMap<>();
		propMap.put("{auth_authen}", Arrays.asList(sender));
		UpdatedMailMessage umm = new UpdatedMailMessage(propMap,
				Mime4JHelper.parse(new ByteArrayInputStream(buffer.toByteArray())));
		umm.envelopSender = Optional.of(sender);

		return umm;
	}

	private void assertEnvelop(UpdatedMailMessage mm, SmtpAddress sender) {
		assertTrue(mm.envelopSender.isPresent());
		assertTrue(sender.getEmailAddress().equals(mm.envelopSender.get()));
	}

	private void assertMessage(UpdatedMailMessage mm, SmtpAddress from, SmtpAddress recipient) {
		assertMessageFrom(mm, from);
		assertMessageTo(mm, recipient);
	}

	private void assertMessageFrom(UpdatedMailMessage mm, SmtpAddress from) {
		ItemValue<User> userbyEmail = getServiceUser(domainUid).byEmail(from.getEmailAddress());
		assertTrue(userbyEmail.value.emails.stream().map(e -> e.address)
				.filter(e -> mm.getMessage().getFrom().get(0).getAddress().equals(e)).findAny().isPresent());
	}

	private void assertMessageTo(UpdatedMailMessage mm, SmtpAddress recipient) {
		assertTrue(mm.getMessage().getTo().flatten().stream()
				.anyMatch(m -> m.getAddress().equalsIgnoreCase(recipient.getEmailAddress())));
	}

	private void assertMessageHeaders(UpdatedMailMessage mm, SmtpAddress sender, Verb v) {
		Field hSender = mm.getMessage().getHeader().getField("Sender");
		if (v == Verb.SendOnBehalf) {
			assertNotNull(hSender);
			assertTrue(hSender.getBody().contains(sender.getEmailAddress()));
		} else {
			assertNull(hSender);
		}
	}

	private boolean smtpError(UpdatedMailMessage mm) {
		return mm.errorStatus == IMilterListener.Status.DELEGATION_ACL_FAIL;
	}

}
