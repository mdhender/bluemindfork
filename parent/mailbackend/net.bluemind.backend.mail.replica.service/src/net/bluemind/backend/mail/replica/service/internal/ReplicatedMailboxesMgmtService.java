/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.partitions.CyrusBoxes;
import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.SearchQuery;
import net.bluemind.backend.mail.api.SearchQuery.SearchScope;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.IReplicatedMailboxesMgmt;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxRecordItemUri;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.api.ResolvedMailbox;
import net.bluemind.backend.mail.replica.persistence.MailboxRecordStore;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class ReplicatedMailboxesMgmtService implements IReplicatedMailboxesMgmt {

	private static final Logger logger = LoggerFactory.getLogger(ReplicatedMailboxesMgmtService.class);
	private final BmContext context;

	public ReplicatedMailboxesMgmtService(BmContext context) {
		this.context = context;
	}

	@Override
	public Set<MailboxRecordItemUri> getBodyGuidReferences(String guid) {
		Set<MailboxRecordItemUri> refs = new HashSet<>();
		readRecordsByGuid(guid, refs);
		return refs;
	}

	@Override
	public List<Set<MailboxRecordItemUri>> getImapUidReferences(String mailbox, String replicatedMailboxUid, Long uid) {
		String recordsUid = IMailReplicaUids.mboxRecords(replicatedMailboxUid);
		DataSource ds = DataSourceRouter.get(context, recordsUid);
		ContainerStore cs = new ContainerStore(context, ds, context.getSecurityContext());
		Container recordsContainer;
		try {
			recordsContainer = cs.get(recordsUid);
		} catch (SQLException e1) {
			throw new ServerFault("Cannot find records container of folder " + replicatedMailboxUid,
					ErrorCode.SQL_ERROR);
		}

		IMailboxes mailboxesApi = context.su().provider().instance(IMailboxes.class, recordsContainer.domainUid);
		ItemValue<Mailbox> mailboxIv = mailboxesApi.getComplete(recordsContainer.owner);
		if (mailbox == null) {
			throw ServerFault.notFound("mailbox of " + recordsContainer.owner + " not found");
		}
		String subtreeContainerUid = IMailReplicaUids.subtreeUid(recordsContainer.domainUid, mailboxIv);
		try {
			Container subtreeContainer = cs.get(subtreeContainerUid);
			if (subtreeContainer == null) {
				throw ServerFault.notFound("subtree " + subtreeContainerUid);
			}
			MailboxRecordStore store = new MailboxRecordStore(ds, recordsContainer, subtreeContainer);
			List<Set<MailboxRecordItemUri>> refs = new ArrayList<>();
			try {
				String guid = store.getImapUidReferences(uid, mailbox);
				refs.add(getBodyGuidReferences(guid));
			} catch (SQLException e) {
				logger.warn("Cannot read referenced message body", e);
			}
			return refs;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<Set<MailboxRecordItemUri>> queryReferences(String mailbox, String query) {
		List<Set<MailboxRecordItemUri>> refs = new ArrayList<>();

		MailboxFolderSearchQuery indexQuery = new MailboxFolderSearchQuery();
		indexQuery.query = new SearchQuery();
		indexQuery.query.maxResults = 999;
		indexQuery.query.query = query;
		indexQuery.query.scope = new SearchScope();
		SearchResult searchResult = MailIndexActivator.getService().searchItems(mailbox, indexQuery);

		Set<String> bodyGuids = new HashSet<>();
		searchResult.results.forEach(ret -> {
			context.getAllMailboxDataSource().forEach(ds -> {
				MailboxRecordStore store = new MailboxRecordStore(ds);
				try {
					MailboxRecord mailboxRecord = store.get(Item.create(null, ret.itemId));
					if (mailboxRecord != null) {
						bodyGuids.add(mailboxRecord.messageBody);
					}
				} catch (SQLException e) {
					logger.warn("Cannot read referenced message body", e);
				}
			});

		});
		bodyGuids.forEach(guid -> refs.add(getBodyGuidReferences(guid)));

		return refs;
	}

	private void readRecordsByGuid(String guid, Set<MailboxRecordItemUri> refs) {
		select(refs, store -> {
			try {
				return store.getBodyGuidReferences(guid);
			} catch (SQLException e) {
				logger.warn("Cannot read referenced message bodies by imap-uid", e);
				return Collections.emptyList();
			}
		});
	}

	private void select(Set<MailboxRecordItemUri> refs, Function<MailboxRecordStore, List<MailboxRecordItemUri>> func) {
		context.getAllMailboxDataSource().forEach(ds -> {
			MailboxRecordStore store = new MailboxRecordStore(ds);
			refs.addAll(func.apply(store));
		});
	}

	private static final String apiCacheKey(ReplicatedBox box) {
		return box.partition + "!" + box.local;
	}

	private static class ResolutionDTO {
		final ReplicatedBox box;
		final String name;

		private ResolutionDTO(ReplicatedBox rb, String n) {
			this.box = rb;
			this.name = n;
		}
	}

	@Override
	public List<ResolvedMailbox> resolve(List<String> names) {
		int len = names.size();
		if (len > 100) {
			logger.info("Resolving {} name(s)", len);
		}
		Map<String, IDbReplicatedMailboxes> subApis = new HashMap<>();
		IServiceProvider apis = context.provider();
		return names.stream().map(cyrusName -> {
			int exMarkIdx = cyrusName.indexOf('!');
			if (exMarkIdx < 0) {
				return null;
			}
			ReplicatedBox ret = CyrusBoxes.forCyrusMailbox(cyrusName);
			try {
				subApis.computeIfAbsent(apiCacheKey(ret),
						b -> apis.instance(IDbReplicatedMailboxes.class, ret.partition, ret.ns.prefix() + ret.local));
				return new ResolutionDTO(ret, cyrusName);
			} catch (Exception e) {
				logger.warn("Skipping '{}' ({}): {}", cyrusName, ret, e.getMessage());
				return null;
			}
		}).filter(Objects::nonNull).map(dto -> {
			ItemValue<MailboxReplica> replica = subApis.get(apiCacheKey(dto.box)).byReplicaName(dto.box.fullName());
			if (replica == null) {
				logger.warn("{} not found.", dto.box);
				return null;
			}
			ResolvedMailbox resolved = new ResolvedMailbox();
			resolved.desc = dto.box.asDescriptor();
			resolved.partition = dto.box.partition;
			resolved.replica = replica;
			if (dto.box.ns == Namespace.users) {

			}
			return resolved;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

}
