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

import java.util.Objects;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchy;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchyMgmt;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;

public class InternalContainersHierarchyMgmtService implements IInternalContainersFlatHierarchyMgmt {

	private static final Logger logger = LoggerFactory.getLogger(InternalContainersHierarchyMgmtService.class);
	private final BmContext context;
	private final String ownerUid;
	private final String domainUid;

	public InternalContainersHierarchyMgmtService(BmContext context, String ownerUid, String domainUid) {
		this.context = context;
		this.ownerUid = ownerUid;
		this.domainUid = domainUid;
	}

	@Override
	public void init() {
		String hierUid = IFlatHierarchyUids.getIdentifier(ownerUid, domainUid);
		DataSource ds = context.getDataSource();
		IDirectory dirApi = context.provider().instance(IDirectory.class, domainUid);
		DirEntry entry = dirApi.findByEntryUid(ownerUid);
		String loc = null;
		if (entry != null && entry.dataLocation != null) {
			loc = entry.dataLocation;
			ds = context.getMailboxDataSource(entry.dataLocation);
			Objects.requireNonNull(ds, "Missing datasource for " + entry.dataLocation);
		}
		final String resolvedLoc = loc;

		if (entry != null && entry.dataLocation == null && entry.kind.hasMailbox()) {
			logger.warn("Skip for now as {} has no datalocation", entry);
			return;
		}

		ContainerStore store = new ContainerStore(context, ds, context.getSecurityContext());
		ContainerStore dirStore = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());
		Container hierCont = Container.create(hierUid, IFlatHierarchyUids.TYPE,
				ownerUid + "@" + domainUid + " container hierarchy", ownerUid);
		hierCont.domainUid = domainUid;
		hierCont.defaultContainer = true;

		JdbcAbstractStore.doOrFail(() -> {
			Container existing = store.get(hierCont.uid);
			if (existing == null) {
				store.create(hierCont);
			} else {
				store.update(hierCont.uid, hierCont.name, hierCont.defaultContainer);
			}
			return null;
		});
		if (resolvedLoc != null) {
			JdbcAbstractStore.doOrFail(() -> {
				dirStore.createOrUpdateContainerLocation(hierCont, resolvedLoc);
				return null;
			});
		}
		logger.info("***** Containers hierarchy init for owner: {} domainUid: {}, loc: {}", ownerUid, domainUid,
				resolvedLoc);
	}

	@Override
	public void delete() {
		logger.info("***** Containers hierarchy delete for owner: {} domainUid: {}", ownerUid, domainUid);
		IDomains domainService = context.provider().instance(IDomains.class);
		ItemValue<Domain> domainVal = domainService.get(domainUid);
		if (domainVal == null) {
			logger.warn(
					"Containers hierarchy cannot be delete for owner: {} domainUid: {} because domain does not exists",
					ownerUid, domainUid);
			return;
		}

		IContainers contApi = context.provider().instance(IContainers.class);
		String hierUid = IFlatHierarchyUids.getIdentifier(ownerUid, domainUid);
		ContainerDescriptor cd = contApi.getIfPresent(hierUid);
		if (cd != null) {
			IInternalContainersFlatHierarchy hierApi = context.provider()
					.instance(IInternalContainersFlatHierarchy.class, cd.domainUid, cd.owner);
			hierApi.reset();
			contApi.delete(hierUid);
		}
	}

}
