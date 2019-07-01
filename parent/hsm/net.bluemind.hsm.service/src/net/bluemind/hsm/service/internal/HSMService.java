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
package net.bluemind.hsm.service.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.ByteStreams;

import net.bluemind.backend.mail.replica.indexing.IMailIndexService;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.hsm.api.IHSM;
import net.bluemind.hsm.api.Promote;
import net.bluemind.hsm.api.TierChangeResult;
import net.bluemind.hsm.processor.HSMContext;
import net.bluemind.hsm.processor.HSMContext.HSMLoginContext;
import net.bluemind.hsm.processor.HSMRunStats;
import net.bluemind.hsm.processor.commands.PromoteCommand;
import net.bluemind.hsm.storage.IHSMStorage;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.StoreClient;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class HSMService implements IHSM {

	private static final Logger logger = LoggerFactory.getLogger(HSMService.class);

	private BmContext bmContext;
	private SecurityContext securityContext;
	private ItemValue<Domain> domainValue;

	public HSMService(BmContext context, Container mboxContainer, ItemValue<Domain> domainValue) {
		this.bmContext = context;
		this.domainValue = domainValue;
		this.securityContext = context.getSecurityContext();
	}

	@Override
	public byte[] fetch(String mailboxUid, String hsmId) throws ServerFault {
		HSMContext context = getHSMContext();
		IHSMStorage storage = context.getHSMStorage();
		logger.debug("[{}] Fetch mboxUid {}, hsmId {}", securityContext.getSubject(), mailboxUid, hsmId);

		RBACManager.forContext(bmContext).forContainer(IMailboxAclUids.uidForMailbox(mailboxUid))
				.check(Verb.Read.name());

		try (InputStream is = storage.peek(context.getSecurityContext().getContainerUid(), mailboxUid, hsmId);) {
			return ByteStreams.toByteArray(is);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public double getSize(String mailboxUid) throws ServerFault {
		if (bmContext.su().provider().instance(IMailboxes.class, domainValue.uid).getComplete(mailboxUid) == null) {
			throw new ServerFault("Not found", ErrorCode.NOT_FOUND);
		}

		Optional<IMailIndexService> indexer = RecordIndexActivator.getIndexer();
		if (indexer.isPresent()) {
			return indexer.get().getArchivedMailSum(mailboxUid);
		}
		return 0d;
	}

	@Override
	public void copy(String sourceMailboxUid, String destMailboxUid, List<String> hsmIds) throws ServerFault {
		HSMContext context = getHSMContext();
		IHSMStorage storage = context.getHSMStorage();
		try {
			for (String hsmId : hsmIds) {
				storage.copy(domainValue.uid, sourceMailboxUid, destMailboxUid, hsmId);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e);
		}
	}

	private HSMContext getHSMContext() throws ServerFault {

		ItemValue<User> user = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IUser.class, bmContext.getSecurityContext().getContainerUid())
				.getComplete(bmContext.getSecurityContext().getSubject());
		ItemValue<Server> server = bmContext.su().provider().instance(IServer.class, InstallationId.getIdentifier())
				.getComplete(user.value.dataLocation);

		HSMLoginContext loginContext = new HSMLoginContext(user.value.login, user.uid, server.value.address());
		return HSMContext.get(securityContext, loginContext);
	}

	@Override
	public List<TierChangeResult> promoteMultiple(List<Promote> promote) throws ServerFault {

		HSMContext context = getHSMContext();

		List<TierChangeResult> ret = new ArrayList<TierChangeResult>(promote.size());

		Multimap<String, Promote> toPromote = HashMultimap.create();

		promote.forEach(p -> {
			toPromote.put(p.folder, p);
		});

		toPromote.asMap().forEach((folder, items) -> {
			try {
				ret.addAll(promote(context, folder, items));
			} catch (ServerFault e) {
				logger.error(e.getMessage(), e);
			}
		});

		return ret;
	}

	private List<TierChangeResult> promote(HSMContext context, String folderPath, Collection<Promote> promote) {
		try (StoreClient sc = context.connect(folderPath)) {
			PromoteCommand pc = new PromoteCommand(folderPath, sc, context, promote);
			HSMRunStats stats = new HSMRunStats();
			return pc.run(stats);
		} catch (IMAPException e) {
			throw new ServerFault(e);
		}
	}
}
