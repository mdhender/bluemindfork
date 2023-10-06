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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.stream.Field;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
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
import net.bluemind.mailflow.rbe.CoreClientContext;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.IMilterListener;
import net.bluemind.milter.SmtpAddress;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.milter.cache.DomainAliasCache;
import net.bluemind.mime4j.common.Mime4JHelper;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class DelegationActionTests {

	private IClientContext cliContext;
	private ItemValue<Mailbox> mailboxFrom;
	private ItemValue<Mailbox> mailboxSendAs;
	private ItemValue<Mailbox> mailboxSendOnBehalf;
	private ItemValue<Mailbox> mailboxWrite;
	private ItemValue<Mailbox> mailboxRead;
	private ItemValue<Mailbox> mailboxAll;
	private ItemValue<Server> dataLocation;
	private List<String> emails = new ArrayList<>();

	private static final String DOMAIN_ALIAS = "test.bm.lan";
	private static final String DOMAIN_ALIAS_1 = "test1.bm.lan";

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

		dataLocation = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).getComplete(PopulateHelper.FAKE_CYRUS_IP);

		ItemValue<Domain> domainItem = PopulateHelper.createTestDomain(DOMAIN_ALIAS);
		domainItem.value.aliases.add(DOMAIN_ALIAS);
		domainItem.value.aliases.add(DOMAIN_ALIAS_1);
		DomainAliasCacheFiller.addDomain(domainItem);

		cliContext = new CoreClientContext(domainItem);
		assertNotNull(cliContext.getSenderDomain().value.defaultAlias);

		PopulateHelper.addUser("hpot", domainItem.uid);
		PopulateHelper.addUser("dumbledore", domainItem.uid);
		PopulateHelper.addUser("mcgonagal", domainItem.uid);
		PopulateHelper.addUser("malefoy", domainItem.uid);
		PopulateHelper.addUser("rogue", domainItem.uid);
		PopulateHelper.addUser("voldemort", domainItem.uid);

		emails.add("hpot@" + DOMAIN_ALIAS);
		emails.add("h-pot@" + DOMAIN_ALIAS);
		emails.add("harry@" + DOMAIN_ALIAS_1);
		mailboxFrom = getServiceMailbox(cliContext.getSenderDomain().uid).byEmail(emails.get(0));
		mailboxFrom.value.emails.add(Email.create(emails.get(1), false));
		mailboxFrom.value.emails.add(Email.create(emails.get(2), false));
		getServiceMailbox(cliContext.getSenderDomain().uid).update(mailboxFrom.uid, mailboxFrom.value);

		mailboxSendAs = getServiceMailbox(cliContext.getSenderDomain().uid).byEmail("dumbledore@test.bm.lan");
		mailboxSendOnBehalf = getServiceMailbox(cliContext.getSenderDomain().uid).byEmail("mcgonagal@test.bm.lan");
		mailboxWrite = getServiceMailbox(cliContext.getSenderDomain().uid).byEmail("rogue@test.bm.lan");
		mailboxRead = getServiceMailbox(cliContext.getSenderDomain().uid).byEmail("malefoy@test.bm.lan");
		mailboxAll = getServiceMailbox(cliContext.getSenderDomain().uid).byEmail("voldemort@test.bm.lan");
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

		String senderAddress = mailboxSendOnBehalf.value.defaultEmail().address;
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
		String senderAddress = emails.get(0);
		UpdatedMailMessage mm = loadTemplate("sendMe_sameAlias.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(emails.get(1));
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
		String senderAddress = emails.get(0);
		UpdatedMailMessage mm = loadTemplate("sendMe_otherAlias.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(emails.get(2));
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, null);
		assertFalse(smtpError(mm));
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

	private void assertMessageFrom(UpdatedMailMessage mm, SmtpAddress sender) {
		assertTrue(mm.getMessage().getFrom().stream()
				.anyMatch(m -> m.getAddress().equalsIgnoreCase(sender.getEmailAddress())));
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
