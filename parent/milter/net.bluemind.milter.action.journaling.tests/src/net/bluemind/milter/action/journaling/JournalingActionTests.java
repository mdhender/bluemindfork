/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.milter.action.journaling;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.james.mime4j.stream.Field;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailflow.rbe.CoreClientContext;
import net.bluemind.mailflow.rbe.IClientContext;
import net.bluemind.milter.SmtpAddress;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.action.MilterActionException;
import net.bluemind.milter.action.UpdatedMailMessage;
import net.bluemind.mime4j.common.Mime4JHelper;

public class JournalingActionTests {

	private IClientContext cliContext;

	private static final String DOMAIN_ALIAS = "test.bm.lan";
	private static final String JOURNAL_TARGET_EMAIL = "journal@other.bm.lan";

	public static class DomainAliasCacheFiller extends DomainAliasCache {
		public static void addDomain(ItemValue<Domain> domain) {
			domain.value.aliases.forEach(alias -> domainCache.put(alias, domain));
		}
	}

	@Before
	public void before() throws Exception {
		Domain domain = new Domain();
		domain.aliases = new HashSet<>(Arrays.asList(DOMAIN_ALIAS));
		domain.defaultAlias = DOMAIN_ALIAS;
		ItemValue<Domain> domainItem = ItemValue.create(Item.create(UUID.randomUUID().toString(), null), domain);
		DomainAliasCacheFiller.addDomain(domainItem);

		cliContext = new CoreClientContext(domainItem);
		assertNotNull(cliContext.getSenderDomain().value.defaultAlias);

		domain = new Domain();
		String domainAlias = "ext.test.bm.lan";
		domain.aliases = new HashSet<>(Arrays.asList(domainAlias));
		domain.defaultAlias = domainAlias;
		domainItem = ItemValue.create(Item.create(UUID.randomUUID().toString(), null), domain);
		DomainAliasCacheFiller.addDomain(domainItem);

		domain = new Domain();
		domainAlias = "ext2.test.bm.lan";
		domain.aliases = new HashSet<>(Arrays.asList(domainAlias));
		domain.defaultAlias = domainAlias;
		domainItem = ItemValue.create(Item.create(UUID.randomUUID().toString(), null), domain);
		DomainAliasCacheFiller.addDomain(domainItem);
	}

	@Test
	public void testFilterOnSender() throws Exception {
		UpdatedMailMessage mm = loadTemplate("filterOnSender.eml");

		SmtpAddress sender = new SmtpAddress("hpot@test.bm.lan");
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, sender, recipient);

		Map<String, String> configuration = createConfiguration(sender.getEmailAddress(), JOURNAL_TARGET_EMAIL);
		new JournalingAction().execute(mm, configuration, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, sender, recipient);
		assertMessageHeaders(mm, sender, Arrays.asList(recipient));
	}

	@Test
	public void testFilterOnSender_extDomain() throws Exception {
		UpdatedMailMessage mm = loadTemplate("filterOnSender_extDomain.eml");

		SmtpAddress sender = new SmtpAddress("hpot@ext.test.bm.lan");
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, sender, recipient);

		Map<String, String> configuration = createConfiguration(sender.getEmailAddress(), JOURNAL_TARGET_EMAIL);
		new JournalingAction().execute(mm, configuration, null, cliContext);

		assertEnvelop(mm, sender);
		assertMessage(mm, sender, recipient);
		assertMessageHeaders(mm, sender, Arrays.asList(recipient));
	}

	@Test
	public void testFilterOnSenderAndRcpt() throws Exception {
		UpdatedMailMessage mm = loadTemplate("filterOnSenderAndRcpt.eml");

		SmtpAddress sender = new SmtpAddress("hpot@test.bm.lan");
		SmtpAddress recipient = new SmtpAddress("cdig@test.bm.lan");

		assertMessage(mm, sender, recipient);

		String emailFiltered = sender.getEmailAddress().concat(JournalingAction.EMAILS_SEPARATOR)
				.concat(recipient.getEmailAddress());
		Map<String, String> configuration = createConfiguration(emailFiltered, JOURNAL_TARGET_EMAIL);
		new JournalingAction().execute(mm, configuration, null, cliContext);

		assertEnvelop(mm, sender, recipient);
		assertMessage(mm, sender, recipient);
		assertMessageHeaders(mm, sender, Arrays.asList(recipient));
	}

	@Test
	public void testFilterOnSenderAndRcpt_extDomain() throws Exception {
		UpdatedMailMessage mm = loadTemplate("filterOnSenderAndRcpt_extDomain.eml");

		SmtpAddress sender = new SmtpAddress("hpot@ext.test.bm.lan");
		SmtpAddress recipient = new SmtpAddress("cdig@ext2.test.bm.lan");

		assertMessage(mm, sender, recipient);

		String emailFiltered = sender.getEmailAddress().concat(JournalingAction.EMAILS_SEPARATOR)
				.concat(recipient.getEmailAddress());
		Map<String, String> configuration = createConfiguration(emailFiltered, JOURNAL_TARGET_EMAIL);
		new JournalingAction().execute(mm, configuration, null, cliContext);

		assertEnvelop(mm, sender, recipient);
		assertMessage(mm, sender, recipient);
		assertMessageHeaders(mm, sender, Arrays.asList(recipient));
	}

	@Test
	public void testFilterOnSingleRcpt() throws Exception {
		UpdatedMailMessage mm = loadTemplate("filterOnSingleRcpt.eml");

		SmtpAddress sender = new SmtpAddress("hgran@test.bm.lan");
		SmtpAddress recipient = new SmtpAddress("hpot@test.bm.lan");

		assertMessage(mm, sender, recipient);

		Map<String, String> configuration = createConfiguration(recipient.getEmailAddress(), JOURNAL_TARGET_EMAIL);
		new JournalingAction().execute(mm, configuration, null, cliContext);

		assertEnvelop(mm, recipient);
		assertMessage(mm, sender, recipient);
		assertMessageHeaders(mm, sender, Arrays.asList(recipient));
	}

	@Test
	public void testFilterOnSingleRcpt_extDomain() throws Exception {
		UpdatedMailMessage mm = loadTemplate("filterOnSingleRcpt_extDomain.eml");

		SmtpAddress sender = new SmtpAddress("hgran@ext2.test.bm.lan");
		SmtpAddress recipient = new SmtpAddress("hpot@ext.test.bm.lan");

		assertMessage(mm, sender, recipient);

		Map<String, String> configuration = createConfiguration(recipient.getEmailAddress(), JOURNAL_TARGET_EMAIL);
		new JournalingAction().execute(mm, configuration, null, cliContext);

		assertEnvelop(mm, recipient);
		assertMessage(mm, sender, recipient);
		assertMessageHeaders(mm, sender, Arrays.asList(recipient));
	}

	@Test
	public void testFilterOnTwoRcpt_extDomain() throws Exception {
		UpdatedMailMessage mm = loadTemplate("filterOnTwoRcpt_extDomain.eml");

		SmtpAddress sender = new SmtpAddress("hgran@test.bm.lan");
		SmtpAddress recipient1 = new SmtpAddress("hpot@ext.test.bm.lan");
		SmtpAddress recipient2 = new SmtpAddress("cdig@ext2.test.bm.lan");

		assertMessageFrom(mm, sender);
		assertMessageTo(mm, recipient1);
		assertMessageTo(mm, recipient2);
		assertMessageToNotJournal(mm);

		String emailsFiltered = recipient1.getEmailAddress().concat(JournalingAction.EMAILS_SEPARATOR)
				.concat(recipient2.getEmailAddress());
		Map<String, String> configuration = createConfiguration(emailsFiltered, JOURNAL_TARGET_EMAIL);
		new JournalingAction().execute(mm, configuration, null, cliContext);

		assertEnvelop(mm, recipient1, recipient2);

		assertMessageFrom(mm, sender);
		assertMessageTo(mm, recipient1);
		assertMessageTo(mm, recipient2);
		assertMessageToNotJournal(mm);
		assertMessageHeaders(mm, sender, Arrays.asList(recipient1, recipient2));
	}

	@Test
	public void testNoFilter() throws Exception {
		UpdatedMailMessage mm = loadTemplate("noFilter.eml");

		SmtpAddress sender = new SmtpAddress("hpot@test.bm.lan");
		SmtpAddress recipient = new SmtpAddress("hgran@test.bm.lan");

		assertMessage(mm, sender, recipient);

		Map<String, String> configuration = createConfiguration(null, JOURNAL_TARGET_EMAIL);
		new JournalingAction().execute(mm, configuration, null, cliContext);

		assertEnvelop(mm, sender, recipient);
		assertMessage(mm, sender, recipient);
		assertMessageHeaders(mm, sender, Arrays.asList(recipient));
	}

	@Test
	public void testNoFilter_extDomain() throws Exception {
		String template = "noFilter_extDomain.eml";
		UpdatedMailMessage mm = loadTemplate(template);

		SmtpAddress sender = new SmtpAddress("hpot@ext.test.bm.lan");
		SmtpAddress recipient = new SmtpAddress("hgran@ext.test.bm.lan");

		assertMessage(mm, sender, recipient);

		Map<String, String> configuration = createConfiguration(null, JOURNAL_TARGET_EMAIL);
		try {
			new JournalingAction().execute(mm, configuration, null, cliContext);
			fail("error must occurs");
		} catch (MilterActionException e) {
			assertTrue(true);
		}

		assertMessage(mm, sender, recipient);
	}

	private UpdatedMailMessage loadTemplate(String name) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		InputStream is = this.getClass().getResourceAsStream("/templates/" + name);

		int nRead;
		byte[] data = new byte[1024];

		while ((nRead = is.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		return new UpdatedMailMessage(Collections.emptyMap(),
				Mime4JHelper.parse(new ByteArrayInputStream(buffer.toByteArray())));
	}

	private Map<String, String> createConfiguration(String emailsFiltered, String targetEmail) {
		Map<String, String> configuration = new HashMap<>();
		if (emailsFiltered != null) {
			configuration.put(JournalingAction.EMAILS_FILTERED_KEY, emailsFiltered);
		}
		configuration.put(JournalingAction.TARGET_EMAIL_KEY, targetEmail);
		return configuration;
	}

	private void assertEnvelop(UpdatedMailMessage mm, SmtpAddress noreply) {
		assertTrue(mm.envelopSender.isPresent());
		assertTrue(JournalingAction.NO_REPLY.concat(noreply.getDomainPart()).equals(mm.envelopSender.get()));
		assertTrue(mm.addRcpt.stream().anyMatch(r -> r.equalsIgnoreCase(JOURNAL_TARGET_EMAIL)));
	}

	private void assertEnvelop(UpdatedMailMessage mm, SmtpAddress env1, SmtpAddress env2) {
		assertTrue(mm.envelopSender.isPresent());
		assertTrue(JournalingAction.NO_REPLY.concat(env1.getDomainPart()).equals(mm.envelopSender.get())
				|| JournalingAction.NO_REPLY.concat(env2.getDomainPart()).equals(mm.envelopSender.get()));
		assertTrue(mm.addRcpt.stream().anyMatch(r -> r.equalsIgnoreCase(JOURNAL_TARGET_EMAIL)));
	}

	private void assertMessage(UpdatedMailMessage mm, SmtpAddress sender, SmtpAddress recipient) {
		assertMessageFrom(mm, sender);
		assertMessageTo(mm, recipient);
		assertMessageToNotJournal(mm);
	}

	private void assertMessageFrom(UpdatedMailMessage mm, SmtpAddress sender) {
		assertTrue(mm.getMessage().getFrom().stream()
				.anyMatch(m -> m.getAddress().equalsIgnoreCase(sender.getEmailAddress())));
	}

	private void assertMessageTo(UpdatedMailMessage mm, SmtpAddress recipient) {
		assertTrue(mm.getMessage().getTo().flatten().stream()
				.anyMatch(m -> m.getAddress().equalsIgnoreCase(recipient.getEmailAddress())));
	}

	private void assertMessageHeaders(UpdatedMailMessage mm, SmtpAddress sender, List<SmtpAddress> recipients) {
		Field hFrom = mm.getMessage().getHeader().getField("X-BM-Journaling-Orig-From");
		assertNotNull(hFrom);
		assertTrue(hFrom.getBody().contains(sender.getEmailAddress()));

		Field hTo = mm.getMessage().getHeader().getField("X-BM-Journaling-Orig-To");
		assertNotNull(hTo);
		for (SmtpAddress recipient : recipients) {
			assertTrue(hTo.getBody().contains(recipient.getEmailAddress()));
		}
	}

	private void assertMessageToNotJournal(UpdatedMailMessage mm) {
		assertTrue(mm.getMessage().getTo().flatten().stream()
				.noneMatch(m -> m.getAddress().equalsIgnoreCase(JOURNAL_TARGET_EMAIL)));
	}

}
