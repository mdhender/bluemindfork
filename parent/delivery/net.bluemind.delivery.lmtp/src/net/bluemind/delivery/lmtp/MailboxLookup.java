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
package net.bluemind.delivery.lmtp;

import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.address.LenientAddressBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;

public class MailboxLookup {

	private final IServiceProvider sp;
	private static final Logger logger = LoggerFactory.getLogger(MailboxLookup.class);

	private static final Cache<String, ResolvedBox> cache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES)
			.recordStats().build();

	public static class Reg implements ICacheRegistration {

		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register("lmtp.resolved.boxes", cache);
		}

	}

	public MailboxLookup(ApiProv prov) {
		this.sp = prov.system();
	}

	public ResolvedBox lookupEmail(String recipient) {
		ResolvedBox recip = cache.getIfPresent(recipient);
		if (recip == null) {
			recip = lookupEmail0(recipient);
			if (recip != null) {
				cache.put(recipient, recip);
			}
		}
		return recip;
	}

	private ResolvedBox lookupEmail0(String recipient) {
		Mailbox m4jBox = LenientAddressBuilder.DEFAULT.parseMailbox(recipient);
		if (m4jBox == null) {
			logger.warn("Cannot parse '{}'", m4jBox);
			return null;
		}
		IDomains domApi = sp.instance(IDomains.class);
		ItemValue<Domain> dom = domApi.findByNameOrAliases(m4jBox.getDomain());
		if (dom == null) {
			return null;
		}
		IDirectory dirApi = sp.instance(IDirectory.class, m4jBox.getDomain());
		DirEntry entry = dirApi.getByEmail(recipient);
		if (entry == null) {
			return null;
		}
		IMailboxes mboxApi = sp.instance(IMailboxes.class, dom.uid);
		logger.debug("Lookup {}@{} ({})", entry.entryUid, dom.uid, entry.email);
		ItemValue<net.bluemind.mailbox.api.Mailbox> mailbox = mboxApi.getComplete(entry.entryUid);
		if (mailbox == null) {
			return null;
		}
		return new ResolvedBox(entry, mailbox, dom);
	}

}
