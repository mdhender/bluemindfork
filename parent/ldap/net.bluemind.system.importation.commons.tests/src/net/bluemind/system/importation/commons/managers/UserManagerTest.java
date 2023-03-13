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
package net.bluemind.system.importation.commons.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.junit.Test;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.Email;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.user.api.User;

public class UserManagerTest {
	private class UserManagerTestImpl extends UserManager {
		public UserManagerTestImpl(ItemValue<Domain> domain) {
			super(domain, new DefaultEntry());
			user = ItemValue.create("" + System.nanoTime(), new User());
			user.value.login = "login-" + System.nanoTime();
			user.value.routing = Routing.internal;
			user.value.contactInfos = new VCard();
		}

		public UserManagerTestImpl(ItemValue<Domain> domain, Entry entry) {
			super(domain, entry);
			user = ItemValue.create("" + System.nanoTime(), new User());
			user.value.login = "login-" + System.nanoTime();
			user.value.routing = Routing.internal;
			user.value.contactInfos = new VCard();
		}

		@Override
		public List<? extends UuidMapper> getUserGroupsMemberGuid(LdapConnection ldapCon) {
			return Collections.emptyList();
		}

		@Override
		public String getExternalId(IImportLogger importLogger) {
			return null;
		}

		@Override
		protected void setLoginFromDefaultAttribute(IImportLogger importLogger)
				throws LdapInvalidAttributeValueException {
		}

		@Override
		protected void manageArchived() {
		}

		@Override
		protected void setMailRouting() {
		}

		@Override
		protected List<String> getEmails() {
			return Collections.emptyList();
		}

		@Override
		protected Parameters getDirectoryParameters() {
			return null;
		}

		@Override
		protected List<IEntityEnhancer> getEntityEnhancerHooks() {
			return Collections.emptyList();
		}

		@Override
		protected void manageContactInfos() throws LdapInvalidAttributeValueException {
		}

		@Override
		protected void manageQuota(IImportLogger importLogger) throws LdapInvalidAttributeValueException {
		}
	}

	private ItemValue<Domain> getDomain() {
		Domain domain = Domain.create("domain.tld", "label", "description",
				new HashSet<>(Arrays.asList("domain-alias1.tld", "domain-alias2.tld")));
		return ItemValue.create(Item.create(domain.name, 0), domain);
	}

	@Test
	public void externalEmailsOnly() {
		List<String> emails = Arrays.asList("toto@yahoo.fr", "titi@gmail.com");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.manageEmails(emails);

		assertEquals(Routing.none, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());
		for (Email e : umt.user.value.emails) {
			assertTrue(emails.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void externalEmailsOnlyWithDuplicate() {
		List<String> emails = Arrays.asList("toto@yahoo.fr", "titi@gmail.com", "toto@yahoo.fr");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.manageEmails(emails);

		assertEquals(Routing.none, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());
		for (Email e : umt.user.value.emails) {
			assertTrue(emails.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void internalEmailsOnly() {
		List<String> emails = Arrays.asList("toto@domain.tld", "titi@domain-alias1.tld", "tata@domain-alias2.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(4, umt.user.value.emails.size());
		List<String> expectedEmail = new ArrayList<>(Arrays.asList(umt.user.value.login + "@" + domain.value.name));
		expectedEmail.addAll(emails);
		for (Email e : umt.user.value.emails) {
			assertTrue(expectedEmail.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void internalEmailsOnlyWithDuplicate() {
		List<String> emails = Arrays.asList("toto@domain.tld", "titi@domain-alias1.tld", "toto@domain.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(3, umt.user.value.emails.size());

		List<String> exectedEmails = new ArrayList<>(Arrays.asList(umt.user.value.login + "@" + domain.value.name));
		exectedEmails.addAll(emails);
		for (Email e : umt.user.value.emails) {
			assertTrue(exectedEmails.contains(e.address));
			assertFalse(e.allAliases);

			if (e.address.equals(emails.get(0))) {
				assertTrue(e.isDefault);
			}
		}
	}

	@Test
	public void internalEmailsAllAliasesExpended() {
		List<String> emails = Arrays.asList("toto@domain.tld", "toto@domain-alias1.tld", "toto@domain-alias2.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.allAliases);
				assertFalse(email.isDefault);
			} else {
				assertEquals(emails.get(0), email.address);
				assertTrue(umt.user.value.emails.iterator().next().isDefault);
				assertTrue(umt.user.value.emails.iterator().next().allAliases);
			}
		}
	}

	@Test
	public void internalEmailsAllAliasesExpendedDefaultInDomainAlias() {
		List<String> emails = Arrays.asList("toto@domain-alias1.tld", "toto@domain.tld", "toto@domain-alias2.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
			} else {
				assertEquals(emails.get(0), email.address);
				assertTrue(umt.user.value.emails.iterator().next().isDefault);
				assertTrue(umt.user.value.emails.iterator().next().allAliases);
			}
		}
	}

	@Test
	public void mixedEmails() {
		List<String> emails = Arrays.asList("toto@gmail.com", "toto@domain-alias1.tld", "titi@yahoo.fr");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
			} else {
				assertEquals(emails.get(1), email.address);
				assertTrue(umt.user.value.emails.iterator().next().isDefault);
				assertFalse(umt.user.value.emails.iterator().next().allAliases);
			}
		}
	}

	@Test
	public void emailWithoutDomainPart() {
		List<String> emails = Arrays.asList("toto");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(2, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
			} else {
				assertEquals(emails.get(0) + "@" + domain.value.name, email.address);
				assertTrue(umt.user.value.emails.iterator().next().isDefault);
				assertTrue(umt.user.value.emails.iterator().next().allAliases);
			}
		}
	}

	@Test
	public void internalEmailsWithAndWithoutDomainPart() {
		List<String> emails = Arrays.asList("toto@domain-alias1.tld", "titi", "toto");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(3, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.startsWith(emails.get(1) + "@")) {
				assertEquals(emails.get(1) + "@" + domain.value.name, email.address);
				assertTrue(email.allAliases);
				assertFalse(email.isDefault);
				continue;
			} else if (email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.allAliases);
				assertFalse(email.isDefault);
				continue;
			}

			assertEquals(emails.get(0), email.address);
			assertTrue(email.allAliases);
			assertTrue(email.isDefault);
		}
	}

	@Test
	public void defaultEmailNotAllAliases() {
		List<String> emails = Arrays.asList("toto@domain.tld", "titi@domain.tld", "toto@domain-alias1.tld");

		ItemValue<Domain> domain = getDomain();

		UserManagerTestImpl umt = new UserManagerTestImpl(domain);
		umt.manageEmails(emails);

		assertEquals(Routing.internal, umt.user.value.routing);

		assertEquals(4, umt.user.value.emails.size());

		for (Email email : umt.user.value.emails) {
			if (email.address.equals("toto@domain.tld")) {
				assertTrue(email.isDefault);
				assertFalse(email.allAliases);
				continue;
			} else if (email.address.equals("titi@domain.tld") || email.address.equals("toto@domain-alias1.tld")
					|| email.address.equals(umt.user.value.login + "@" + domain.value.name)) {
				assertFalse(email.isDefault);
				assertFalse(email.allAliases);
				continue;
			}

			fail("Unknown address: " + email.address);
		}
	}

	@Test
	public void getUpdatedMailFilter_userCreated() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = true;
		assertNotNull(umt.getUpdatedMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void getUpdatedMailFilter_userUpdated() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = false;
		assertNotNull(umt.getUpdatedMailFilter());
		assertFalse(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void getUpdatedMailFilter_setExternalMailRouting() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.setExternalMailRouting();
		assertNotNull(umt.getUpdatedMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void getUpdatedMailFilter_noneRouting() {
		List<String> emails = Arrays.asList("toto@yahoo.fr", "titi@gmail.com");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.manageEmails(emails);

		assertEquals(Routing.none, umt.user.value.routing);
		assertNotNull(umt.getUpdatedMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void update_createUserNullMailFilter() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.update(null, null);

		assertNotNull(umt.getMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
		assertEquals(umt.getUpdatedMailFilter().get(), new MailFilter());
	}

	@Test
	public void update_updateUserNullMailFilter() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.update(ItemValue.create(Item.create("test", null), new User()), null);

		assertNotNull(umt.getMailFilter());
		assertFalse(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void update_createUserWithMailFilter() {
		MailFilter mf = new MailFilter();
		mf.forwarding.enabled = true;
		mf.forwarding.emails.add("test@domain.tld");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.update(null, mf);

		assertNotNull(umt.getMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
		assertEquals(umt.getUpdatedMailFilter().get(), new MailFilter());
	}

	@Test
	public void update_updateUserWithMailFilter() {
		MailFilter mf = new MailFilter();
		mf.forwarding.enabled = true;
		mf.forwarding.emails.add("test@domain.tld");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.update(ItemValue.create(Item.create("test", null), new User()), mf);

		assertEquals(Routing.none, umt.user.value.routing);
		assertNotNull(umt.getMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());
		assertFalse(umt.getUpdatedMailFilter().get().forwarding.enabled);
	}

	@Test
	public void setMailFilter_nullMailFilter() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = false;
		umt.setMailFilter(null);

		assertNotNull(umt.getMailFilter());
		assertFalse(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void setMailFilter_sameFilter() {
		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = false;
		umt.setMailFilter(new MailFilter());

		assertNotNull(umt.getMailFilter());
		assertFalse(umt.getUpdatedMailFilter().isPresent());
	}

	@Test
	public void setMailFilter_updatedFilter() {
		MailFilter mf = new MailFilter();
		mf.forwarding.enabled = true;
		mf.forwarding.emails.add("test@domain.tld");

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain());
		umt.create = false;
		umt.setMailFilter(mf);

		assertNotNull(umt.getMailFilter());
		assertTrue(umt.getUpdatedMailFilter().isPresent());

		mf = new MailFilter();
		mf.forwarding.enabled = true;
		mf.forwarding.emails.add("test@domain.tld");
		assertEquals(umt.getUpdatedMailFilter().get(), mf);
	}

	@Test
	public void certificate_pem() {
		String certificate = "MIIFwzCCA6ugAwIBAgIUVTSFATfec/mVyk95Yu8jhQJjEhcwDQYJKoZIhvcNAQELBQAwcTELMAkG" //
				+ "A1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwIVG91bG91c2UxETAPBgNVBAoMCEJs" //
				+ "dWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAYBgNVBAMMEWxkYXBhZC5pbXBvcnQudGxkMB4XDTIz" //
				+ "MDMwMzE1MjMxOFoXDTI0MDMwMjE1MjMxOFowcTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5j" //
				+ "ZTERMA8GA1UEBwwIVG91bG91c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMx" //
				+ "GjAYBgNVBAMMEWxkYXBhZC5pbXBvcnQudGxkMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKC" //
				+ "AgEA3SqvSmLU+mnqo11RAYExZ2hT61pJ0vBjGSJ+gIOVgve2Vw8QHWgWs3C/ff8kGiD6F3c/+qzk" //
				+ "Upd65ZcOBMwcnPwDk2rGRbchVCrTwjePyGhWxoC7Mi/RlpRTkc1Q84v0vZ3KthzsCXIMSgRDRnZ4" //
				+ "cmwuj90EN+7tb0BS5HRBdeG921OeIK02DJaO3uqRfC9mnR8Urd1hwqy0nLP7AMOOSE5264+slXPe" //
				+ "yeQg5uTwQFkAV2vZCsjEKS7id82UCQc2BWp+6sMlCZAFXmU1ue2rzohKbAMmfqQZLX5/rTVY4p4U" //
				+ "O+KA8RKaekURt0s7iqOJ/7ANILwdmKEYxNBWuXOLJ8rINl7AI61IOY2tX79jGHacZ/h8dkn14RC9" //
				+ "DKn2w1l8iFQc5tl76MDqaq4KFp6jz6BHCbCfcpziMZGFCK9dcvL+QEflck7iAOd1Gcnj5Az19AxN" //
				+ "a4lL+5VXMOblV6SHz2WyxxlxD9RDa9Opr44rpPUOPsfumS5JbTk4YbwIszi2wFioN+s8EcO/lAh6" //
				+ "ysOTcotdxMg3Bp1VBPkpf4UFJpY6rIdSyHhRt/ymVDx7ohQhfJ1sfSqbNGWVCI+Mk5c4zBXMjPRl" //
				+ "05J9jUuz+JOrMVfaAy71ZF6sZKiQLmeo3w4WEnxX6hDtBhbURjTP2AEdqfN1Y8rlvffWmumFKJyL" //
				+ "nGcCAwEAAaNTMFEwHQYDVR0OBBYEFBicOubB3xEds8WI8DPLrSwxm4P+MB8GA1UdIwQYMBaAFBic" //
				+ "OubB3xEds8WI8DPLrSwxm4P+MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggIBAAN6" //
				+ "mJtKIW2vaRlh9Fwa6g2XIi81YjGO7jti2jotaXFuh0lkxs/IEMfQd+WRjjoHRJmWV30t5abW8weM" //
				+ "FaxUDHAzA9SL5zjlKl5D99F7wC4gy82yOLnhQ1jP5m7XrqbFEQT/AukLnrbawG1kgwVsp+w7Jqdz" //
				+ "PnWDBmd36mmUF5ebIF6dtgvN2L7PFtYVKr/SEa55D4Gdo8i0Jle5/EmYX0IuxLyUmJiUhX03Lexi" //
				+ "uAix96TFWLl3lhFgA3VdtPVqebHibuGHojnLh59d851TM4CB/EuLBgw1/ZM2Gx3ipccuxSZQeHUH" //
				+ "Wq6FiGmCukw7k5S+XOGVZN5cddhV2b04IKDDIMR18uMuUAa0nLOKouDG+0ml/5dmI/tjtYPlF5jT" //
				+ "LQ8hG7bT3LIoXtnyXG1H7hca6YvhOtrlXxShJRp3/CKin/lzrorcp1u1nEwukSFbJJeTVbJ/pU4f" //
				+ "ZNkfJrFfdVuthCb4TgrpYMXkHmdivWMxdoE0HwQTYxXoDjqSVYLuFxnjBNw1JTrQn7ak62d9AKkR" //
				+ "LC7/kw2WCrFoUptC7/kT50htFOCEcXBVGar9YeV1M8LWDLmOQMSjSBO2RYKmGKZHZ5XVvEcFQTyv" //
				+ "WdOlQ32UB2v/lXHXgdayjcszlR/N8xJTZ6ylMgeLA5Jpz8dvGPdk+T0HJiN/zC5jBP8u0qBy";

		Entry entry = new DefaultEntry();
		entry.put("usercertificate;binary", Base64.getDecoder().decode(certificate));

		User user = new User();

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain(), entry);
		umt.update(ItemValue.create(Item.create("test", null), user), new MailFilter());

		assertEquals(1, user.contactInfos.security.key.parameters.size());
		assertEquals("MEDIATYPE", user.contactInfos.security.key.parameters.get(0).label);
		assertEquals("application/x-pem-file", user.contactInfos.security.key.parameters.get(0).value);
		assertEquals("-----BEGIN CERTIFICATE-----" + certificate + "-----END CERTIFICATE-----",
				user.contactInfos.security.key.value.replace("\n", ""));
	}

	@Test
	public void certificate_pkcs7() {
		String pkcs7 = "MIIF8gYJKoZIhvcNAQcCoIIF4zCCBd8CAQExADALBgkqhkiG9w0BBwGgggXHMIIF" //
				+ "wzCCA6ugAwIBAgIUVTSFATfec/mVyk95Yu8jhQJjEhcwDQYJKoZIhvcNAQELBQAw" //
				+ "cTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwIVG91bG91" //
				+ "c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAYBgNVBAMM" //
				+ "EWxkYXBhZC5pbXBvcnQudGxkMB4XDTIzMDMwMzE1MjMxOFoXDTI0MDMwMjE1MjMx" //
				+ "OFowcTELMAkGA1UEBhMCRlIxDzANBgNVBAgMBkZyYW5jZTERMA8GA1UEBwwIVG91" //
				+ "bG91c2UxETAPBgNVBAoMCEJsdWVNaW5kMQ8wDQYDVQQLDAZKVW5pdHMxGjAYBgNV" //
				+ "BAMMEWxkYXBhZC5pbXBvcnQudGxkMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIIC" //
				+ "CgKCAgEA3SqvSmLU+mnqo11RAYExZ2hT61pJ0vBjGSJ+gIOVgve2Vw8QHWgWs3C/" //
				+ "ff8kGiD6F3c/+qzkUpd65ZcOBMwcnPwDk2rGRbchVCrTwjePyGhWxoC7Mi/RlpRT" //
				+ "kc1Q84v0vZ3KthzsCXIMSgRDRnZ4cmwuj90EN+7tb0BS5HRBdeG921OeIK02DJaO" //
				+ "3uqRfC9mnR8Urd1hwqy0nLP7AMOOSE5264+slXPeyeQg5uTwQFkAV2vZCsjEKS7i" //
				+ "d82UCQc2BWp+6sMlCZAFXmU1ue2rzohKbAMmfqQZLX5/rTVY4p4UO+KA8RKaekUR" //
				+ "t0s7iqOJ/7ANILwdmKEYxNBWuXOLJ8rINl7AI61IOY2tX79jGHacZ/h8dkn14RC9" //
				+ "DKn2w1l8iFQc5tl76MDqaq4KFp6jz6BHCbCfcpziMZGFCK9dcvL+QEflck7iAOd1" //
				+ "Gcnj5Az19AxNa4lL+5VXMOblV6SHz2WyxxlxD9RDa9Opr44rpPUOPsfumS5JbTk4" //
				+ "YbwIszi2wFioN+s8EcO/lAh6ysOTcotdxMg3Bp1VBPkpf4UFJpY6rIdSyHhRt/ym" //
				+ "VDx7ohQhfJ1sfSqbNGWVCI+Mk5c4zBXMjPRl05J9jUuz+JOrMVfaAy71ZF6sZKiQ" //
				+ "Lmeo3w4WEnxX6hDtBhbURjTP2AEdqfN1Y8rlvffWmumFKJyLnGcCAwEAAaNTMFEw" //
				+ "HQYDVR0OBBYEFBicOubB3xEds8WI8DPLrSwxm4P+MB8GA1UdIwQYMBaAFBicOubB" //
				+ "3xEds8WI8DPLrSwxm4P+MA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQAD" //
				+ "ggIBAAN6mJtKIW2vaRlh9Fwa6g2XIi81YjGO7jti2jotaXFuh0lkxs/IEMfQd+WR" //
				+ "jjoHRJmWV30t5abW8weMFaxUDHAzA9SL5zjlKl5D99F7wC4gy82yOLnhQ1jP5m7X" //
				+ "rqbFEQT/AukLnrbawG1kgwVsp+w7JqdzPnWDBmd36mmUF5ebIF6dtgvN2L7PFtYV" //
				+ "Kr/SEa55D4Gdo8i0Jle5/EmYX0IuxLyUmJiUhX03LexiuAix96TFWLl3lhFgA3Vd" //
				+ "tPVqebHibuGHojnLh59d851TM4CB/EuLBgw1/ZM2Gx3ipccuxSZQeHUHWq6FiGmC" //
				+ "ukw7k5S+XOGVZN5cddhV2b04IKDDIMR18uMuUAa0nLOKouDG+0ml/5dmI/tjtYPl" //
				+ "F5jTLQ8hG7bT3LIoXtnyXG1H7hca6YvhOtrlXxShJRp3/CKin/lzrorcp1u1nEwu" //
				+ "kSFbJJeTVbJ/pU4fZNkfJrFfdVuthCb4TgrpYMXkHmdivWMxdoE0HwQTYxXoDjqS" //
				+ "VYLuFxnjBNw1JTrQn7ak62d9AKkRLC7/kw2WCrFoUptC7/kT50htFOCEcXBVGar9" //
				+ "YeV1M8LWDLmOQMSjSBO2RYKmGKZHZ5XVvEcFQTyvWdOlQ32UB2v/lXHXgdayjcsz" //
				+ "lR/N8xJTZ6ylMgeLA5Jpz8dvGPdk+T0HJiN/zC5jBP8u0qByMQA=";

		Entry entry = new DefaultEntry();
		entry.put("usersmimecertificate", Base64.getDecoder().decode(pkcs7));

		User user = new User();

		UserManagerTestImpl umt = new UserManagerTestImpl(getDomain(), entry);
		umt.update(ItemValue.create(Item.create("test", null), user), new MailFilter());

		assertEquals(1, user.contactInfos.security.key.parameters.size());
		assertEquals("MEDIATYPE", user.contactInfos.security.key.parameters.get(0).label);
		assertEquals("application/pkcs7-mime", user.contactInfos.security.key.parameters.get(0).value);
		assertEquals("-----BEGIN PKCS7-----" + pkcs7 + "-----END PKCS7-----",
				user.contactInfos.security.key.value.replace("\n", ""));
	}
}
