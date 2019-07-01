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
package net.bluemind.role.service;

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.role.api.IRoles;
import net.bluemind.role.provider.IRolesProvider;
import net.bluemind.role.provider.IRolesVerifier;
import net.bluemind.role.service.internal.RolesService;

public class RolesFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IRoles> {

	static List<IRolesProvider> loadFactories() {
		RunnableExtensionLoader<IRolesProvider> rel = new RunnableExtensionLoader<IRolesProvider>();
		return rel.loadExtensions("net.bluemind.role", "provider", "roles-provider", "class");
	}

	static List<IRolesVerifier> loadValidators() {
		RunnableExtensionLoader<IRolesVerifier> rel = new RunnableExtensionLoader<IRolesVerifier>();
		return rel.loadExtensions("net.bluemind.role", "verifier", "verifier", "impl");
	}

	@Override
	public Class<IRoles> factoryClass() {
		return IRoles.class;
	}

	@Override
	public IRoles instance(BmContext context, String... params) throws ServerFault {
		// TODO check admin ?
		return new RolesService(context, RolesServiceActivator.providers, RolesServiceActivator.resolver,
				RolesServiceActivator.validators);
	}

}
