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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifacts;
import net.bluemind.backend.mail.replica.persistence.MailboxSubStore;
import net.bluemind.backend.mail.replica.persistence.QuotaStore;
import net.bluemind.backend.mail.replica.persistence.SeenOverlayStore;
import net.bluemind.backend.mail.replica.persistence.SieveScriptStore;
import net.bluemind.backend.mail.replica.service.internal.CyrusArtifactsService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class CyrusArtifactsServiceFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<ICyrusReplicationArtifacts> {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(CyrusAnnotationsServiceFactory.class);

	@Override
	public Class<ICyrusReplicationArtifacts> factoryClass() {
		return ICyrusReplicationArtifacts.class;
	}

	@Override
	public ICyrusReplicationArtifacts instance(BmContext context, String... params) throws ServerFault {
		if (params.length < 1) {
			throw new ServerFault("Missing userId param");
		}

		String userId = params[0];
		String[] splitted = userId.split("@");
		String localPart = splitted[0];
		String domain = "";
		if (splitted.length == 2) {
			domain = splitted[1];
		}
		String email = userId.replace('^', '.');
		DataSource pool;
		if ("".equals(domain) || ("bmhiddensysadmin@" + domain).equals(email)) {
			pool = context.getDataSource();
		} else {
			IDomains domApi = context.getServiceProvider().instance(IDomains.class);
			ItemValue<Domain> theDomain = domApi.findByNameOrAliases(domain);
			if (theDomain == null) {
				throw ServerFault.notFound("Replicated domain:" + domain + " not found");
			}
			IDirectory dirApi = context.provider().instance(IDirectory.class, theDomain.uid);
			DirEntry entry = dirApi.getByEmail(email);
			if (entry == null) {
				// user with routing none has no email but has a mailbox
				IMailboxes mboxApi = context.su().provider().instance(IMailboxes.class, theDomain.uid);
				ItemValue<Mailbox> mbox = mboxApi.byName(localPart);
				if (mbox != null) {
					entry = dirApi.findByEntryUid(mbox.uid);
				}
				if (entry == null) {
					throw ServerFault.notFound("DirEntry with email '" + email + "' (or mailbox name) not found");
				}
			}
			pool = context.getMailboxDataSource(entry.dataLocation);
		}
		MailboxSubStore sub = new MailboxSubStore(pool);
		QuotaStore qs = new QuotaStore(pool);
		SeenOverlayStore seen = new SeenOverlayStore(pool);
		SieveScriptStore sieve = new SieveScriptStore(pool);
		return new CyrusArtifactsService(userId, sub, qs, seen, sieve);
	}

}
