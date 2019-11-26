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
package net.bluemind.core.container.service.internal;

import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptions;
import net.bluemind.core.container.api.internal.IInternalOwnerSubscriptionsMgmt;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class InternalOwnerSubscriptionsMgmtService implements IInternalOwnerSubscriptionsMgmt {

	private static final Logger logger = LoggerFactory.getLogger(InternalOwnerSubscriptionsMgmtService.class);
	private final BmContext context;
	private final String ownerUid;
	private final String domainUid;

	public InternalOwnerSubscriptionsMgmtService(BmContext context, String ownerUid, String domainUid) {
		this.context = context;
		this.ownerUid = ownerUid;
		this.domainUid = domainUid;
	}

	@Override
	public void init() {
		IDirectory dirApi = context.provider().instance(IDirectory.class, domainUid);
		DirEntry entry = dirApi.findByEntryUid(ownerUid);
		if (entry == null) {
			logger.warn("Entry not found with uid {}", ownerUid);
			return;
		}
		if (entry.kind != Kind.USER || entry.system) {
			return;
		}
		DataSource ds = null;
		logger.info("***** Owner subscriptions init for user {}: {} @: {}", entry.displayName, ownerUid, domainUid);
		if (entry.dataLocation == null) {
			logger.warn("Using directory datasource for {} subscriptions", entry);
			ds = context.getDataSource();
		} else {
			ds = context.getMailboxDataSource(entry.dataLocation);
		}

		Objects.requireNonNull(ds, "Missing datasource for " + entry.dataLocation);
		String subsUid = IOwnerSubscriptionUids.getIdentifier(ownerUid, domainUid);

		ContainerStore shardContStore = new ContainerStore(context, ds, context.getSecurityContext());
		ContainerStore dirStore = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());
		Container subsCont = Container.create(subsUid, IOwnerSubscriptionUids.TYPE,
				ownerUid + "@" + domainUid + " subscriptions", ownerUid);
		subsCont.domainUid = domainUid;
		subsCont.defaultContainer = true;
		try {
			// this is a 'transaction' involving 2 databases...
			Container existing = shardContStore.get(subsUid);
			if (existing == null) {
				shardContStore.create(subsCont);
			} else {
				logger.warn("container {} already exist", subsUid);
			}
			dirStore.createContainerLocation(subsCont, entry.dataLocation);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void delete() {
		String subsUid = IOwnerSubscriptionUids.getIdentifier(ownerUid, domainUid);
		IContainers contApi = context.provider().instance(IContainers.class);
		try {
			ContainerDescriptor cd = contApi.get(subsUid);
			IInternalOwnerSubscriptions subsApi = context.provider().instance(IInternalOwnerSubscriptions.class,
					cd.domainUid, cd.owner);
			subsApi.reset();
			contApi.delete(subsUid);
			logger.info("***** Owner subscriptions deleted for: {} @: {}", ownerUid, domainUid);
		} catch (ServerFault sf) {
			if (sf.getCode() == ErrorCode.NOT_FOUND) {
				// this is fine, we might not be dealing with a user
			} else {
				throw sf;
			}
		}
	}

}
