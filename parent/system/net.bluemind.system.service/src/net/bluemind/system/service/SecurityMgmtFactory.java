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
package net.bluemind.system.service;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.api.ISecurityMgmt;
import net.bluemind.system.hook.ISystemHook;
import net.bluemind.system.service.certificate.SecurityMgmt;

public class SecurityMgmtFactory implements ServerSideServiceProvider.IServerSideServiceFactory<ISecurityMgmt> {
	private static final List<ISystemHook> hooks = getHooks();

	private static List<ISystemHook> getHooks() {
		RunnableExtensionLoader<ISystemHook> loader = new RunnableExtensionLoader<ISystemHook>();
		List<ISystemHook> hooks = loader.loadExtensions("net.bluemind.system", "hook", "hook", "class");
		return hooks;

	}

	@Override
	public Class<ISecurityMgmt> factoryClass() {
		return ISecurityMgmt.class;
	}

	@Override
	public ISecurityMgmt instance(BmContext context, String... params) throws ServerFault {
		return new SecurityMgmt(context, hooks);
	}
}
