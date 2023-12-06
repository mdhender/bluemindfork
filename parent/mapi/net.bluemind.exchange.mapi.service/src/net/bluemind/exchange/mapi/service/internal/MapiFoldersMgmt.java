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
package net.bluemind.exchange.mapi.service.internal;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.hierarchy.hook.HierarchyIdsHints;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ContainerModifiableDescriptor;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;
import net.bluemind.exchange.mapi.api.IMapiFolder;
import net.bluemind.exchange.mapi.api.IMapiFoldersMgmt;
import net.bluemind.exchange.mapi.api.MapiFolder;
import net.bluemind.exchange.mapi.api.MapiFolderContainer;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.mapi.hook.IMapiArtifactsHook;
import net.bluemind.exchange.mapi.hook.MapiArtifactsHooks;
import net.bluemind.exchange.mapi.persistence.MapiFoldersStore;

public class MapiFoldersMgmt implements IMapiFoldersMgmt {

	private static final Logger logger = LoggerFactory.getLogger(MapiFoldersMgmt.class);

	private BmContext context;
	private MapiFoldersStore store;
	private MapiReplica replica;
	private String domain;

	public MapiFoldersMgmt(BmContext context, String domain, MapiReplica replica, DataSource storeDs) {
		this.context = context;
		this.replica = replica;
		this.domain = domain;
		this.store = new MapiFoldersStore(storeDs);
	}

	@Override
	public void store(MapiFolder mf) {
		mf.replicaGuid = replica.localReplicaGuid;
		try {
			store.store(mf);

			IContainers contApi = context.provider().instance(IContainers.class);
			setupContainer(contApi, mf);
			for (IMapiArtifactsHook h : MapiArtifactsHooks.get()) {
				h.onMapiFolderStored(domain, replica.mailboxUid, mf);
			}
		} catch (SQLException e1) {
			throw ServerFault.sqlFault(e1);
		}

	}

	private void setupContainer(IContainers contApi, MapiFolder mf) {
		ContainerDescriptor fais = ContainerDescriptor.create(mf.containerUid, mf.displayName, replica.mailboxUid,
				MapiFolderContainer.TYPE, domain, false);
		if (mf.expectedId != null) {
			String hierUid = ContainerHierarchyNode.uidFor(mf.containerUid, MapiFolderContainer.TYPE, domain);
			HierarchyIdsHints.putHint(hierUid, mf.expectedId);
		}
		BaseContainerDescriptor exist = contApi.getLightIfPresent(mf.containerUid);
		if (exist == null) {
			logger.info("Create {} matching folder {}...", fais, mf);
			contApi.create(mf.containerUid, fais);
		}
		IContainerManagement aclApi = context.su().provider().instance(IContainerManagement.class, mf.containerUid);
		aclApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(domain, Verb.All)));
		logger.info("Created container {}", mf.containerUid);
	}

	@Override
	public MapiFolder get(String containerUid) {
		try {
			return store.get(containerUid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void repair(String containerUid) {
		MapiFolder f = get(containerUid);
		IContainers contApi = context.provider().instance(IContainers.class);
		BaseContainerDescriptor existingContainer = null;
		existingContainer = contApi.getLightIfPresent(f.containerUid);
		if (existingContainer == null) {
			setupContainer(contApi, f);
		} else {
			IContainerManagement aclApi = context.su().provider().instance(IContainerManagement.class, containerUid);
			repairName(contApi, existingContainer, f);
			repairAcls(aclApi);
		}
	}

	private void repairName(IContainers contApi, BaseContainerDescriptor existingContainer, MapiFolder folder) {
		if (existingContainer.defaultContainer || !existingContainer.name.equals(folder.displayName)) {
			logger.info("Container {} exists but needs adjustments", folder.containerUid);
			ContainerModifiableDescriptor cmd = new ContainerModifiableDescriptor();
			cmd.defaultContainer = false;
			cmd.name = folder.displayName;
			contApi.update(folder.containerUid, cmd);
		}
	}

	private void repairAcls(IContainerManagement aclApi) {
		List<AccessControlEntry> acls = aclApi.getAccessControlList();
		if (acls.size() != 1 || !acls.get(0).subject.equals(domain) || acls.get(0).verb != Verb.All) {
			aclApi.setAccessControlList(Arrays.asList(AccessControlEntry.create(domain, Verb.All)));
		}
	}

	@Override
	public void delete(String containerUid) {
		logger.info("Deleting mapi folder container {}", containerUid);
		try {
			IMapiFolder toClearApi = context.provider().instance(IMapiFolder.class, containerUid);
			toClearApi.reset();
		} catch (ServerFault sf) {
			// we delete a not existing container
			logger.warn(sf.getMessage());
			return;
		}
		IContainers contApi = context.provider().instance(IContainers.class);
		try {
			store.delete(containerUid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
		contApi.delete(containerUid);
	}

	@Override
	public void deleteAll() {
		IContainers contApi = context.provider().instance(IContainers.class);
		logger.info("Deleting all mapi folders of {} : {}", replica.mailboxUid, MapiFolderContainer.TYPE);
		List<BaseContainerDescriptor> all = contApi
				.allLight(ContainerQuery.ownerAndType(replica.mailboxUid, MapiFolderContainer.TYPE));
		for (BaseContainerDescriptor c : all) {
			delete(c.uid);
		}
	}

}
