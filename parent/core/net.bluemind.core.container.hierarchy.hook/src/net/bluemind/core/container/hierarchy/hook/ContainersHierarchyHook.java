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
package net.bluemind.core.container.hierarchy.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.internal.IInternalContainersFlatHierarchy;
import net.bluemind.core.container.hooks.ContainersHookAdapter;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class ContainersHierarchyHook extends ContainersHookAdapter {

	private static final Logger logger = LoggerFactory.getLogger(ContainersHierarchyHook.class);

	private IInternalContainersFlatHierarchy hierarchy(BmContext ctx, ContainerDescriptor cd, DirEntry owner) {
		if (cd.domainUid == null || cd.owner == null || "global.virt".equals(cd.domainUid)) {
			return null;
		}
		try {
			return ctx.provider().instance(IInternalContainersFlatHierarchy.class, cd.domainUid, cd.owner);
		} catch (ServerFault sf) {
			logger.warn("Missing hierarchy container {} for {}", cd, owner);
			return null;
		}
	}

	private DirEntry getOwner(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		if (cd.owner == null || cd.domainUid == null) {
			return null;
		}
		IDirectory dir = ctx.provider().instance(IDirectory.class, cd.domainUid);
		return dir.findByEntryUid(cd.owner);
	}

	private String uid(ContainerDescriptor cd) {
		return ContainerHierarchyNode.uidFor(cd.uid, cd.type, cd.domainUid);
	}

	@FunctionalInterface
	private static interface HierarchyOperation {
		void accept(IInternalContainersFlatHierarchy hierarchy, DirEntry owner) throws ServerFault;
	}

	private void hierarchyOp(BmContext ctx, ContainerDescriptor cd, HierarchyOperation operation) {
		SystemState state = StateContext.getState();
		if (state == SystemState.CORE_STATE_CLONING) {
			return;
		}
		DirEntry owner = getOwner(ctx, cd);
		if (owner == null) {
			logger.warn("Owner not found in directory, Nothing to do on {} owned by {}", cd.uid, cd.owner);
		} else {
			IInternalContainersFlatHierarchy service = hierarchy(ctx, cd, owner);
			if (service != null) {
				operation.accept(service, owner);
			}
		}
	}

	@Override
	public void onContainerCreated(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		hierarchyOp(ctx, cd, (hier, owner) -> {
			logger.info("Container created {}, should create in hierarchy", cd);
			String hierUid = uid(cd);
			Long expected = HierarchyIdsHints.getHint(hierUid);
			if (expected == null) {
				storeNode(cd, hier, hierUid);
			} else {
				hier.createWithId(expected, hierUid, ContainerHierarchyNode.of(cd));
			}
		});
	}

	@Override
	public void onContainerUpdated(BmContext ctx, ContainerDescriptor prev, ContainerDescriptor cur)
			throws ServerFault {
		hierarchyOp(ctx, cur, (hier, owner) -> {
			logger.info("Container updated from {} to {}, should update in hierarchy", prev, cur);
			String hierUid = uid(cur);
			storeNode(cur, hier, hierUid);
		});

	}

	private void storeNode(ContainerDescriptor cur, IInternalContainersFlatHierarchy hier, String hierUid) {
		ItemValue<ContainerHierarchyNode> existing = hier.getComplete(hierUid);
		if (existing == null) {
			hier.create(hierUid, ContainerHierarchyNode.of(cur));
		} else {
			hier.update(hierUid, ContainerHierarchyNode.of(cur));
		}
	}

	@Override
	public void onContainerSettingsChanged(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		hierarchyOp(ctx, cd, (hier, owner) -> {
			if (owner.kind != DirEntry.Kind.USER) {
				logger.info("Skipping update as owner is {}", owner.kind);
			} else {
				logger.info("Container settings updated for {}, should update in hierarchy", cd);
				hier.update(uid(cd), ContainerHierarchyNode.of(cd));
			}
		});

	}

	@Override
	public void onContainerDeleted(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
		hierarchyOp(ctx, cd, (hier, owner) -> {
			logger.info("Container deleted {}, should delete in hierarchy", cd);
			hier.delete(uid(cd));
		});

	}

}
