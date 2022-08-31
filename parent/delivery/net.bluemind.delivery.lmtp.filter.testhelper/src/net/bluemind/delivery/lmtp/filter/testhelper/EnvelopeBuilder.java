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
package net.bluemind.delivery.lmtp.filter.testhelper;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.LenientAddressBuilder;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.delivery.lmtp.common.LmtpEnvelope;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;

public class EnvelopeBuilder {

	private EnvelopeBuilder() {
	}

	public static LmtpEnvelope forEmails(String... emails) {
		return new LmtpEnvelope("sender@gmail.com",
				Arrays.stream(emails).map(EnvelopeBuilder::lookupEmail).collect(Collectors.toList()));
	}

	public static ResolvedBox lookupEmail(String recipient) {
		IServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		Mailbox m4jBox = LenientAddressBuilder.DEFAULT.parseMailbox(recipient);
		IDomains domApi = prov.instance(IDomains.class);
		ItemValue<Domain> dom = domApi.findByNameOrAliases(m4jBox.getDomain());
		if (dom == null) {
			System.err.println("Domain " + m4jBox.getDomain() + " not found.");
			return null;
		}
		IDirectory dirApi = prov.instance(IDirectory.class, m4jBox.getDomain());
		DirEntry entry = dirApi.getByEmail(recipient);
		if (entry == null) {
			System.err.println("entry not found for " + recipient);
			return null;
		}
		IMailboxes mboxApi = prov.instance(IMailboxes.class, dom.uid);
		ItemValue<net.bluemind.mailbox.api.Mailbox> mailbox = mboxApi.getComplete(entry.entryUid);
		if (mailbox == null) {
			System.err.println("Mailbox not found for entry " + entry);
			return null;
		}
		return new ResolvedBox(entry, mailbox, dom);
	}

}
