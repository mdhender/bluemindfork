/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.core.backup.continuous.mgmt.service.containers.misc;

import java.util.List;

import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ContainerState;
import net.bluemind.core.backup.continuous.mgmt.service.impl.ContainerSync;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;

public class DevicesSync extends ContainerSync {

	private BmContext ctx;

	public DevicesSync(BmContext ctx, BaseContainerDescriptor cont) {
		super(cont);
		this.ctx = ctx;
	}

	@Override
	public void sync(ContainerState state, IBackupStoreFactory target, IServerTaskMonitor contMon) {
		IDevice devApi = ctx.provider().instance(IDevice.class, cont.owner);
		IBackupStore<Device> tgt = target.forContainer(cont);
		List<ItemValue<Device>> devs = devApi.list().values;
		for (ItemValue<Device> iv : devs) {
			tgt.store(iv);
		}
		contMon.log("Stored {} device(s)", devs.size());
	}

	public static class SyncFactory implements ContainerSync.Factory {

		@Override
		public <U> ContainerSync forNode(BmContext ctx, ItemValue<ContainerHierarchyNode> node,
				ItemValue<DirEntryAndValue<U>> owner, ItemValue<Domain> domain) {
			IContainers contApi = ctx.provider().instance(IContainers.class);
			ContainerDescriptor container = contApi.get(node.value.containerUid);
			return new DevicesSync(ctx, container);
		}

	}

}
