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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.eas.http.tests.validators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.api.MessageBody.Recipient;
import net.bluemind.backend.mail.api.MessageBody.RecipientKind;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.eas.http.tests.helpers.CoreEmailHelper;

public class EmailValidator {

	private final String domain;
	private final String serverId;
	private final Map<Element, String> validations;

	private EmailValidator(String domain, String serverId, Map<Element, String> validations) {
		this.serverId = serverId;
		this.validations = validations;
		this.domain = domain;
	}

	public void validate() {
		ItemValue<MailboxItem> mail = CoreEmailHelper.getMailAsRoot(domain, serverId, "user");
		validations(mail);
	}

	public void validate(SecurityContext context) {
		ItemValue<MailboxItem> mail = CoreEmailHelper.getMail(domain, serverId, "user", context);
		validations(mail);
	}

	private void validations(ItemValue<MailboxItem> mail) {
		assertNotNull(mail);
		for (Entry<Element, String> validation : validations.entrySet()) {
			switch (validation.getKey()) {
			case FROM:
				assertRecipient(validation.getValue(), mail, RecipientKind.Originator, RecipientKind.Sender);
				break;
			case TO:
				assertRecipient(validation.getValue(), mail, RecipientKind.Primary, RecipientKind.CarbonCopy,
						RecipientKind.BlindCarbonCopy);
				break;
			case SUBJECT:
				assertEquals(validation.getValue(), mail.value.body.subject);
				break;
			case HEADER:
				String name = validation.getValue().split(":")[0];
				String value = validation.getValue().split(":")[1];
				assertEquals(value, header(mail.value.body.headers, name));
				break;
			case READ:
				if (mail.value.flags.isEmpty()) {
					assertEquals("0", validation.getValue());
				} else if (mail.value.flags.stream()
						.anyMatch(f -> f.value == MailboxItemFlag.System.Seen.value().value)) {
					assertEquals("1", validation.getValue());
				} else {
					throw new UnsupportedOperationException("Read status Not implemented yet for flags : "
							+ mail.value.flags.stream().map(f -> f.value).toList());
				}
				break;
			case BODY:
				throw new UnsupportedOperationException("Not implemented yet");
			}
		}
	}

	private void assertRecipient(String email, ItemValue<MailboxItem> mail, RecipientKind... kinds) {
		boolean found = false;
		for (Recipient recipient : mail.value.body.recipients) {
			if (recipient.address.equalsIgnoreCase(email)) {
				for (RecipientKind kind : kinds) {
					if (kind == recipient.kind) {
						found = true;
					}
				}
			}
		}
		assertTrue("Recipient check for " + email, found);
	}

	private String header(List<Header> headers, String name) {
		return headers.stream().filter(h -> h.name.equalsIgnoreCase(name)).findAny().get().firstValue();
	}

	public static class Builder {
		private final String domain;
		private final String serverId;
		private final Map<Element, String> validations = new HashMap<>();

		public Builder(String domain, String serverId) {
			this.domain = domain;
			this.serverId = serverId;
		}

		public Builder withFrom(String from) {
			validations.put(Element.FROM, from);
			return this;
		}

		public Builder withTo(String to) {
			validations.put(Element.TO, to);
			return this;
		}

		public Builder withSubject(String subject) {
			validations.put(Element.SUBJECT, subject);
			return this;
		}

		public Builder withBody(String body) {
			validations.put(Element.BODY, body);
			return this;
		}

		public Builder withRead(String read) {
			validations.put(Element.READ, read);
			return this;
		}

		public Builder withHeader(String name, String value) {
			validations.put(Element.HEADER, name + ":" + value);
			return this;
		}

		public EmailValidator build() {
			return new EmailValidator(domain, serverId, validations);
		}

	}

	public enum Element {
		FROM, TO, SUBJECT, BODY, HEADER, READ
	}

}
