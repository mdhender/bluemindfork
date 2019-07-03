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
package net.bluemind.group.service;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.service.AbstractDirServiceFactory;
import net.bluemind.domain.api.Domain;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.hook.IGroupHook;
import net.bluemind.group.service.internal.GroupService;

public class GroupServiceFactory extends AbstractDirServiceFactory<IGroup>
		implements ServerSideServiceProvider.IServerSideServiceFactory<IGroup> {

	private static final List<IGroupHook> groupHooks = getHooks();

	static List<IGroupHook> getHooks() {
		RunnableExtensionLoader<IGroupHook> loader = new RunnableExtensionLoader<IGroupHook>();
		List<IGroupHook> hooks = loader.loadExtensions("net.bluemind.group.hook", "grouphook", "hook", "impl");
		return hooks;
	}

	@Override
	public Class<IGroup> factoryClass() {
		return IGroup.class;
	}

	@Override
	protected IGroup instanceImpl(BmContext context, ItemValue<Domain> domainValue, Container container)
			throws ServerFault {
		return new GroupService(context, domainValue, container, groupHooks);
	}
}
