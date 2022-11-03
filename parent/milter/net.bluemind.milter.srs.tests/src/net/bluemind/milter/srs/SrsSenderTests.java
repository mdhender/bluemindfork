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

import org.apache.james.mime4j.dom.Header;
import org.apache.james.mime4j.message.HeaderImpl;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.stream.RawField;
import org.junit.Before;
import org.junit.Test;

import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.milter.MilterHeaders;
import net.bluemind.milter.action.DomainAliasCache;
import net.bluemind.milter.action.MilterPreAction;
import net.bluemind.milter.action.UpdatedMailMessage;

public class SrsSenderTests {
	public static class DomainAliasCacheFiller extends DomainAliasCache {
		public static void addDomain(ItemValue<Domain> domain) {
			domain.value.aliases.forEach(alias -> domainCache.put(alias, domain));
			domainCache.put(domain.uid, domain);
		}
	}

	private String domainUid;

	@Before
	public void before() {
		domainUid = UUID.randomUUID().toString() + ".internal";
		Domain domain = getDomain(domainUid, false);

		fillDomainCache(domainUid, domain);
	}

	private void fillDomainCache(String domainUid, Domain domain) {
		DomainAliasCacheFiller.addDomain(ItemValue.create(Item.create(domainUid, null), domain));
	}

	private Domain getDomain(String domainUid, boolean domainUidIsAlias) {
		Domain domain = new Domain();
		domain.defaultAlias = "domain.tld";
		domain.aliases = new HashSet<>(Arrays.asList("domain.tld", "alias.tld"));
		if (domainUidIsAlias) {
			domain.aliases.add(domainUid);
		}

		return domain;
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

		// No auth, no default domain, null header
		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Null auth, no default domain, null header
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList((String) null));
		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Empty auth, no default domain, null header
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList(""));
		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Auth with no domain, no default domain, null header
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login"));
		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Auth with no domain, default domain, null header
		updateMailMessage.envelopSender = Optional.empty();
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login"));
		assertTrue(SrsSender.build("default-domain.tld").execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@default-domain.tld")).orElse(false));

		// Auth with no domain, default domain id domainUid, null header
		updateMailMessage.envelopSender = Optional.empty();
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login"));
		assertTrue(SrsSender.build(domainUid).execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@domain.tld")).orElse(false));

		// Auth with domain, default domain, null header
		updateMailMessage.envelopSender = Optional.empty();
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login@domain.tld"));
		assertTrue(SrsSender.build("default-domain.tld").execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@domain.tld")).orElse(false));

		// Auth with domain, default domain, null header
		updateMailMessage = new UpdatedMailMessage(new HashMap<>(), new MessageImpl());
		updateMailMessage.properties.put("{mail_addr}", Arrays.asList("sender@ext-domain.tld"));
		updateMailMessage.properties.put("{rcpt_addr}", Arrays.asList("rcpt@ext-domain.tld", "rcpt@domain.tld"));

		// Auth with domain UID, default domain, null header
		updateMailMessage.envelopSender = Optional.empty();
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login@" + domainUid));
		assertTrue(SrsSender.build("default-domain.tld").execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@domain.tld")).orElse(false));

		Header header = new HeaderImpl();
		header.setField(new RawField(MilterHeaders.SIEVE_REDIRECT, "login@header-domain.tld"));
		updateMailMessage.getMessage().setHeader(header);

		// Auth with domain, default domain, header
		updateMailMessage.envelopSender = Optional.empty();
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login@domain.tld"));
		assertTrue(SrsSender.build("default-domain.tld").execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@domain.tld")).orElse(false));

		// Auth with domain UID, default domain, header
		updateMailMessage.envelopSender = Optional.empty();
		updateMailMessage.properties.put("{auth_authen}", Arrays.asList("login@" + domainUid));
		assertTrue(SrsSender.build("default-domain.tld").execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@domain.tld")).orElse(false));
	}

	@Test
	public void execute_nonLocalSender_nonLocalRcpt_domainUidIsAlias() {
		Domain domain = getDomain(domainUid, true);
		fillDomainCache(domainUid, domain);

		execute_nonLocalSender_nonLocalRcpt();
	}

	@Test
	public void execute_nonLocalSender_nonLocalRcpt_redirectHeader() {
		UpdatedMailMessage updateMailMessage = new UpdatedMailMessage(new HashMap<>(), new MessageImpl());
		updateMailMessage.properties.put("{mail_addr}", Arrays.asList("sender@ext-domain.tld"));
		updateMailMessage.properties.put("{rcpt_addr}", Arrays.asList("rcpt@ext-domain.tld", "rcpt@domain.tld"));

		// null header
		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// null X-BM-redirect-for
		Header header = new HeaderImpl();
		updateMailMessage.getMessage().setHeader(header);

		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		header = new HeaderImpl();
		header.setField(new RawField(MilterHeaders.SIEVE_REDIRECT, null));
		updateMailMessage.getMessage().setHeader(header);

		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// empty X-BM-redirect-for
		header = new HeaderImpl();
		header.setField(new RawField(MilterHeaders.SIEVE_REDIRECT, ""));
		updateMailMessage.getMessage().setHeader(header);

		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// invalid X-BM-redirect-for
		header = new HeaderImpl();
		header.setField(new RawField(MilterHeaders.SIEVE_REDIRECT, "invalid"));
		updateMailMessage.getMessage().setHeader(header);

		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Invalid domain X-BM-redirect-for
		updateMailMessage.envelopSender = Optional.empty();
		header = new HeaderImpl();
		header.setField(new RawField(MilterHeaders.SIEVE_REDIRECT, "john@"));
		updateMailMessage.getMessage().setHeader(header);

		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// Unknown domain X-BM-redirect-for
		updateMailMessage.envelopSender = Optional.empty();
		header = new HeaderImpl();
		header.setField(new RawField(MilterHeaders.SIEVE_REDIRECT, "john@unknown.tld"));
		updateMailMessage.getMessage().setHeader(header);

		assertFalse(srsSender().execute(updateMailMessage));
		assertFalse(updateMailMessage.envelopSender.isPresent());

		// domain alias X-BM-redirect-for
		header = new HeaderImpl();
		header.setField(new RawField(MilterHeaders.SIEVE_REDIRECT, "john@domain.tld"));
		updateMailMessage.getMessage().setHeader(header);

		assertTrue(srsSender().execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@domain.tld")).orElse(false));

		updateMailMessage.envelopSender = Optional.empty();
		header = new HeaderImpl();
		header.setField(new RawField(MilterHeaders.SIEVE_REDIRECT, "john@alias.tld"));
		updateMailMessage.getMessage().setHeader(header);

		assertTrue(srsSender().execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@alias.tld")).orElse(false));

		// domain uid X-BM-redirect-for
		updateMailMessage.envelopSender = Optional.empty();
		header = new HeaderImpl();
		header.setField(new RawField(MilterHeaders.SIEVE_REDIRECT, "john@" + domainUid));
		updateMailMessage.getMessage().setHeader(header);

		assertTrue(srsSender().execute(updateMailMessage));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.startsWith("SRS0=")).orElse(false));
		assertTrue(updateMailMessage.envelopSender.map(sender -> sender.endsWith("@domain.tld")).orElse(false));
	}

	private MilterPreAction srsSender() {
		return new SrsSender.SrsSenderFactory().create();
	}
}