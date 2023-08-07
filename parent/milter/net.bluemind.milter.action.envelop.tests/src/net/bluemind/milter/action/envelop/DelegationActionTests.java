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
package net.bluemind.milter.action.envelop;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.stream.Field;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailflow.rbe.CoreClientContext;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.SmtpAddress;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.action.UpdatedMailMessage;
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
	private ItemValue<Mailbox> mailboxExternal;
	private ItemValue<Server> dataLocation;

	private static final String DOMAIN_ALIAS = "test.bm.lan";

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
		DomainAliasCacheFiller.addDomain(domainItem);

		cliContext = new CoreClientContext(domainItem);
		assertNotNull(cliContext.getSenderDomain().value.defaultAlias);

		String domainAlias = "ext.test.bm.lan";
		ItemValue<Domain> extDomainItem = PopulateHelper.createTestDomain(domainAlias);
		extDomainItem.value.aliases.add(domainAlias);
		DomainAliasCacheFiller.addDomain(extDomainItem);

		mailboxExternal = createOtherMailbox("dudley", extDomainItem);
		mailboxFrom = createMailbox("hpot");

		mailboxSendAs = createMailbox("dumbledore");
		IMailboxes service = getService(cliContext.getSenderDomain().uid);
		service.setMailboxAccessControlList(mailboxFrom.uid,
				Arrays.asList(AccessControlEntry.create(mailboxSendAs.value.defaultEmail().address, Verb.SendAs)));

		mailboxSendOnBehalf = createMailbox("mcgonagal");
		service.setMailboxAccessControlList(mailboxFrom.uid, Arrays.asList(
				AccessControlEntry.create(mailboxSendOnBehalf.value.defaultEmail().address, Verb.SendOnBehalf)));

		mailboxWrite = createMailbox("rogue");
		service.setMailboxAccessControlList(mailboxFrom.uid,
				Arrays.asList(AccessControlEntry.create(mailboxWrite.value.defaultEmail().address, Verb.Write)));

		mailboxRead = createMailbox("malefoy");
		service.setMailboxAccessControlList(mailboxFrom.uid,
				Arrays.asList(AccessControlEntry.create(mailboxRead.value.defaultEmail().address, Verb.Read)));
	}

	private ItemValue<Mailbox> createOtherMailbox(String name, ItemValue<Domain> domain) {
		String mbUid = UUID.randomUUID().toString();

		Mailbox mailbox = new Mailbox();
		mailbox.type = Type.user;
		mailbox.routing = Routing.internal;
		mailbox.dataLocation = dataLocation.uid;

		mailbox.name = name;
		Email e = new Email();
		e.address = mailbox.name + "@" + domain.value.defaultAlias;
		e.allAliases = true;
		e.isDefault = true;
		mailbox.emails = new ArrayList<Email>(1);
		mailbox.emails.add(e);

		IMailboxes service = getService(domain.uid);
		service.create(mbUid, mailbox);
		return service.getComplete(mbUid);
	}

	private IMailboxes getService(String domainUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IMailboxes.class, domainUid);
	}

	private ItemValue<Mailbox> createMailbox(String name) {
		String mbUid = UUID.randomUUID().toString();

		Mailbox mailbox = new Mailbox();
		mailbox.type = Type.user;
		mailbox.routing = Routing.internal;
		mailbox.dataLocation = dataLocation.uid;

		mailbox.name = name;
		Email e = new Email();
		e.address = mailbox.name + "@" + cliContext.getSenderDomain().value.defaultAlias;
		e.allAliases = true;
		e.isDefault = true;
		mailbox.emails = new ArrayList<Email>(1);
		mailbox.emails.add(e);

		IMailboxes service = getService(cliContext.getSenderDomain().uid);
		service.create(mbUid, mailbox);
		return service.getComplete(mbUid);
	}

	@Test
	public void testWithSenderAs() throws Exception {
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
	}

	@Test
	public void testWithSenderOnBehalf() throws Exception {
		String senderAddress = mailboxSendOnBehalf.value.defaultEmail().address;
		UpdatedMailMessage mm = loadTemplate("sendOnBehalf.eml", senderAddress);

		SmtpAddress sender = new SmtpAddress(senderAddress);
		SmtpAddress from = new SmtpAddress(mailboxFrom.value.defaultEmail().address);
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, from, recipient);

		new DelegationAction().execute(mm, null, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, from, recipient);
		assertMessageHeaders(mm, sender, Verb.SendOnBehalf);
	}

	private UpdatedMailMessage loadTemplate(String name, String sender) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		InputStream is = this.getClass().getResourceAsStream("/templates/" + name);

		int nRead;
		byte[] data = new byte[1024];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		UpdatedMailMessage umm = new UpdatedMailMessage(Collections.emptyMap(),
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
		Field hSender = mm.getMessage().getHeader().getField("X-BM-Sender");
		if (v == Verb.SendAs) {
			assertNotNull(hSender);
			assertTrue(hSender.getBody().contains(sender.getEmailAddress()));
		} else {
			assertNull(hSender);
		}
	}

}
