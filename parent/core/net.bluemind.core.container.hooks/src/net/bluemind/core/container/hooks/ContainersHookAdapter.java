/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.core.container.hooks;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.rest.BmContext;

public class ContainersHookAdapter implements IContainersHook {

	@Override
	public void onContainerCreated(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
	}

	@Override
	public void onContainerUpdated(BmContext ctx, ContainerDescriptor prev, ContainerDescriptor cur)
			throws ServerFault {
	}

	@Override
	public void onContainerDeleted(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
	}

	@Override
	public void onContainerSubscriptionsChanged(BmContext ctx, ContainerDescriptor cd, List<String> subs,
			List<String> unsubs) throws ServerFault {
	}

	@Override
	public void onContainerOfflineSyncStatusChanged(BmContext ctx, ContainerDescriptor cd, String subject) {
	}

	@Override
	public void onContainerSettingsChanged(BmContext ctx, ContainerDescriptor cd) throws ServerFault {
	}

}
