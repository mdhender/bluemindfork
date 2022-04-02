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
package net.bluemind.core.backup.continuous.mgmt.service.impl;

import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.container.api.ContainerHierarchyNode;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.domain.api.Domain;

public class ContainerSync {

	public static interface Factory {
		public <U> ContainerSync forNode(BmContext ctx, ItemValue<ContainerHierarchyNode> node,
				ItemValue<DirEntryAndValue<U>> owner, ItemValue<Domain> domain);
	}

	protected final BaseContainerDescriptor cont;

	public ContainerSync(BaseContainerDescriptor cont) {
		this.cont = cont;
	}

	public void sync(ContainerState state, IBackupStoreFactory target, IServerTaskMonitor contMon) {

	}

}
