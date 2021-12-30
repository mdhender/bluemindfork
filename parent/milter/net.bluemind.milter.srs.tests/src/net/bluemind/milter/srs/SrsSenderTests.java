/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.milter.srs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import org.apache.james.mime4j.message.MessageImpl;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.action.MilterPreAction;
import net.bluemind.milter.action.UpdatedMailMessage;

public class SrsSenderTests {
	public static class DomainAliasCacheFiller extends DomainAliasCache {
		public static void addDomain(ItemValue<Domain> domain) {
			domain.value.aliases.forEach(alias -> domainCache.put(alias, domain));
		}
	}

	@Before
	public void before() {
		Domain domain = new Domain();
		domain.aliases = new HashSet<>(Arrays.asList("domain.tld", "alias.tld"));

		DomainAliasCacheFiller.addDomain(ItemValue.create(Item.create(UUID.randomUUID().toString(), null), domain));
	}

	@Test
	public void execute_invalidFrom() {
		UpdatedMailMessage updateMailMessage = new UpdatedMailMessage(new HashMap<>(), new MessageImpl());
		assertFalse(srsSender().execute(updateMailMessage));

		updateMailMessage.properties.put("{mail_addr}", null);
		assertFalse(srsSender().execute(updateMailMessage));

		updateMailMessage.properties.put("{mail_addr}", Arrays.asList((String) null));
		assertFalse(srsSender().execute(updateMailMessage));

		updateMailMessage.properties.put("{mail_addr}", Arrays.asList(""));
		assertFalse(srsSender().execute(updateMailMessage));
	}

	@Test
	public void execute_noRcpt() {
		UpdatedMailMessage updateMailMessage = new UpdatedMailMessage(new HashMap<>(), new MessageImpl());
		updateMailMessage.properties.put("{mail_addr}", Arrays.asList("sender@domain.tld"));
		updateMailMessage.properties.put("{rcpt_addr}", null);

		assertFalse(srsSender().execute(updateMailMessage));
	}

	@Test
	public void execute_localSender() {
		UpdatedMailMessage updateMailMessage = new UpdatedMailMessage(new HashMap<>(), new MessageImpl());
		updateMailMessage.properties.put("{mail_addr}", Arrays.asList("sender@domain.tld"));
		updateMailMessage.properties.put("{rcpt_addr}", Arrays.asList("rcpt@alias.tld"));

		assertFalse(srsSender().execute(updateMailMessage));
	}

	@Test
	public void execute_nonLocalSender_localRcpt() {
		UpdatedMailMessage updateMailMessage = new UpdatedMailMessage(new HashMap<>(), new MessageImpl());
		updateMailMessage.properties.put("{mail_addr}", Arrays.asList("sender@ext-domain.tld"));
		updateMailMessage.properties.put("{rcpt_addr}", Arrays.asList("rcpt@alias.tld", "rcpt@domain.tld"));

		assertFalse(srsSender().execute(updateMailMessage));
	}

	@Test
	public void execute_nonLocalSender_nonLocalRcpt() {
		UpdatedMailMessage updateMailMessage = new UpdatedMailMessage(new HashMap<>(), new MessageImpl());
		updateMailMessage.properties.put("{mail_addr}", Arrays.asList("sender@ext-domain.tld"));
		updateMailMessage.properties.put("{rcpt_addr}", Arrays.asList("rcpt@ext-domain.tld", "rcpt@domain.tld"));

		// No auth and no default domain
		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Null auth and no default domain
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList((String) null));
		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Empty auth and no default domain
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList(""));
		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Auth with no domain and no default domain
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login"));
		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Auth with no domain and default domain
		updateMailMessage.envelopSender = Optional.empty();
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login"));
		assertTrue(SrsSender.build("default-domain.tld").execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@default-domain.tld")).orElse(false));

		// Auth with domain and default domain
		updateMailMessage.envelopSender = Optional.empty();
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login@domain.tld"));
		assertTrue(SrsSender.build("default-domain.tld").execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@domain.tld")).orElse(false));
	}

	private MilterPreAction srsSender() {
		return new SrsSender.SrsSenderFactory().create();
	}
}