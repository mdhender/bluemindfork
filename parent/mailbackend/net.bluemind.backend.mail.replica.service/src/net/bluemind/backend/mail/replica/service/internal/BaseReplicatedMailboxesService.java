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
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.bluemind.backend.mail.api.IBaseMailboxFolders;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxFolderSearchQuery;
import net.bluemind.backend.mail.api.SearchResult;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor.Namespace;
import net.bluemind.backend.mail.replica.persistence.MailboxReplicaStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerChangelog;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemChangelog;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.ItemVersion;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.ChangeLogUtil;
import net.bluemind.core.container.service.internal.ContainerStoreService;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.index.MailIndexActivator;
import net.bluemind.lib.jutf7.UTF7Converter;
import net.bluemind.mailbox.api.IMailboxAclUids;

public class BaseReplicatedMailboxesService implements IBaseMailboxFolders {

	private static final Logger logger = LoggerFactory.getLogger(BaseReplicatedMailboxesService.class);

	protected final BmContext context;
	protected final MailboxReplicaStore replicaStore;
	protected final ContainerStoreService<MailboxReplica> storeService;
	protected final ContainerStore contStore;
	protected final Container container;

	protected final MailboxReplicaRootDescriptor root;
	protected final String dataLocation;
	protected final RBACManager rbac;

	public BaseReplicatedMailboxesService(MailboxReplicaRootDescriptor root, Container cont, BmContext context,
			MailboxReplicaStore store, ContainerStoreService<MailboxReplica> mboxReplicaStore,
			ContainerStore contStore) {
		this.root = root;
		this.container = cont;
		this.context = context;
		this.replicaStore = store;
		this.storeService = mboxReplicaStore;
		this.contStore = contStore;
		this.dataLocation = root.dataLocation;
		this.rbac = RBACManager.forContext(context).forContainer(IMailboxAclUids.uidForMailbox(container.owner));
	}

	protected ItemValue<MailboxFolder> adapt(ItemValue<MailboxReplica> rec) {
		if (rec == null) {
			return null;
		}
		return ItemValue.create(rec, rec.value);
	}

	protected void sanitizeNames(MailboxFolder replica) {
		if (replica.name == null && replica.fullName == null) {
			throw new ServerFault("One of name or fullName must not be null");
		}
		String parentName = null;
		if (replica.fullName != null) {
			replica.fullName = replica.fullName.replace('^', '.');
			replica.fullName = decodeIfUTF7(replica.fullName);
			// use fullName if available & set name+parentUid
			int lastSlash = replica.fullName.lastIndexOf('/');
			if (lastSlash > 0) {
				if (replica.deleted) {
					// keep deleted name
					// fullName/XXX where XXX is a hex encoded timestamp, need
					// for undelete
					// https://www.cyrusimap.org/imap/reference/admin/sop/deleting.html?highlight=undelete
					replica.name = replica.fullName;
				} else {
					parentName = replica.fullName.substring(0, lastSlash);
					replica.name = replica.fullName.substring(lastSlash + 1);
				}
			} else {
				if (root.ns == Namespace.users) {
					replica.name = replica.fullName;
					parentName = null;
				} else {
					replica.name = replica.fullName;
					parentName = root.name.replace('^', '.');
					logger.info("replicaName: {}, parentName: {}", replica.name, parentName);
					if (parentName.equals(replica.name)) {
						parentName = null;
					}
					logger.info("********************* Sanitized mailshare folder with parent set to {}", parentName);
				}
			}

			if (parentName == null) {
				replica.parentUid = null;
			} else {
				ItemValue<MailboxReplica> parent = byReplicaName(parentName);
				if (parent == null) {
					logger.warn("parentName {} not found", parentName);
				} else {
					replica.parentUid = parent.uid;
					logger.info("******** parent set to {}", replica.parentUid);
				}
			}
		} else { // use name + parentUid
			replica.name = replica.name.replace('^', '.');
			replica.name = decodeIfUTF7(replica.name);
			if (replica.parentUid == null) {
				// top lvl folder
				replica.fullName = replica.name;
			} else {
				ItemValue<MailboxReplica> parent = storeService.get(replica.parentUid, null);
				replica.fullName = parent.value.fullName + "/" + replica.name;
			}
		}

	}

	protected String decodeIfUTF7(String s) {
		if (CharMatcher.ascii().matchesAllOf(s)) {
			try {
				return UTF7Converter.decode(s);
			} catch (Error err) { // NOSONAR
				// because jutf7 does not honor onMalformedInput(REPLACE) and
				// Charset.decode
				// throws an Error in that case
				if (logger.isDebugEnabled()) {
					logger.debug("{} looks like utf-7 but it is not", s);
				}
				return s;
			}
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("{} contains non-ascii chars, not decoding as utf7.", s);
			}
			return s;
		}
	}

	private static final Cache<String, String> nameToUid = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.MINUTES).build();

	protected ItemValue<MailboxReplica> byReplicaName(String name) {
		String uid = nameToUid.getIfPresent(name);
		if (uid == null) {
			try {
				uid = replicaStore.byName(decodeIfUTF7(name));
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		}
		if (uid == null) {
			return null;
		} else {
			nameToUid.put(name, uid);
			return getCompleteReplica(uid);
		}
	}

	@Override
	public ItemValue<MailboxFolder> byName(String name) {
		rbac.check(Verb.Read.name());

		ItemValue<MailboxReplica> fetched = byReplicaName(name);
		if (fetched == null) {
			return null;
		} else {
			return adapt(fetched);
		}
	}

	@Override
	public ItemValue<MailboxFolder> getComplete(String uid) {
		rbac.check(Verb.Read.name());

		ItemValue<MailboxReplica> fetched = getCompleteReplica(uid);
		return adapt(fetched);
	}

	protected ItemValue<MailboxReplica> getCompleteReplica(String uid) {
		return Optional.ofNullable(MboxReplicasCache.byUid(uid)).orElseGet(() -> {
			ItemValue<MailboxReplica> fetched = storeService.get(uid, null);
			if (fetched != null) {
				fetched.value.dataLocation = dataLocation;
				MboxReplicasCache.cache(fetched);
			}
			return fetched;
		});
	}

	@Override
	public List<ItemValue<MailboxFolder>> all() {
		rbac.check(Verb.Read.name());

		return storeService.all().stream().map(this::adapt).collect(Collectors.toList());
	}

	@Override
	public ItemChangelog itemChangelog(String itemUid, Long since) throws ServerFault {
		rbac.check(Verb.Read.name());

		return ChangeLogUtil.getItemChangeLog(itemUid, since, context, storeService, container.domainUid);
	}

	@Override
	public ContainerChangelog containerChangelog(Long since) throws ServerFault {
		rbac.check(Verb.Read.name());

		return storeService.changelog(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<String> changeset(Long since) throws ServerFault {
		rbac.check(Verb.Read.name());

		return storeService.changeset(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<Long> changesetById(Long since) throws ServerFault {
		rbac.check(Verb.Read.name());

		return storeService.changesetById(since, Long.MAX_VALUE);
	}

	@Override
	public ContainerChangeset<ItemVersion> filteredChangesetById(Long since, ItemFlagFilter filter) throws ServerFault {
		rbac.check(Verb.Read.name());
		return storeService.changesetById(since, filter);
	}

	@Override
	public long getVersion() throws ServerFault {
		rbac.check(Verb.Read.name());
		return storeService.getVersion();
	}

	@Override
	public SearchResult searchItems(MailboxFolderSearchQuery query) {
		rbac.check(Verb.Read.name());
		RBACManager rbac = null;
		try {
			ContainerStore cs = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());
			rbac = RBACManager.forContext(context).forDomain(container.domainUid)
					.forContainer(cs.get(IMailboxAclUids.uidForMailbox(container.owner)));
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
		rbac.check(Verb.Read.name());

		return MailIndexActivator.getService().searchItems(container.owner, query);
	}

}
