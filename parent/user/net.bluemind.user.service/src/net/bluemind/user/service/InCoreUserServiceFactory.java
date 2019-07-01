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
package net.bluemind.user.service;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.service.AbstractDirServiceFactory;
import net.bluemind.domain.api.Domain;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.user.api.IPasswordUpdater;
import net.bluemind.user.hook.IUserHook;
import net.bluemind.user.service.internal.UserService;

public class InCoreUserServiceFactory extends AbstractDirServiceFactory<IInCoreUser>
		implements ServerSideServiceProvider.IServerSideServiceFactory<IInCoreUser> {

	private static final List<IUserHook> userHooks = getHooks();
	private static final List<IPasswordUpdater> userPasswordUpdaters = getPasswordUpdaters();

	private static List<IUserHook> getHooks() {
		RunnableExtensionLoader<IUserHook> loader = new RunnableExtensionLoader<IUserHook>();
		return loader.loadExtensionsWithPriority("net.bluemind.user.hook", "userhook", "hook", "impl");
	}

	private static List<IPasswordUpdater> getPasswordUpdaters() {
		RunnableExtensionLoader<IPasswordUpdater> loader = new RunnableExtensionLoader<IPasswordUpdater>();
		return loader.loadExtensionsWithPriority("net.bluemind.user", "passwordupdater", "password_updater", "impl");
	}

	@Override
	public Class<IInCoreUser> factoryClass() {
		return IInCoreUser.class;
	}

	@Override
	protected IInCoreUser instanceImpl(BmContext context, ItemValue<Domain> domainValue, Container container)
			throws ServerFault {

		UserService service = new UserService(context, domainValue, container, userHooks, userPasswordUpdaters);
		return service;
	}

}
