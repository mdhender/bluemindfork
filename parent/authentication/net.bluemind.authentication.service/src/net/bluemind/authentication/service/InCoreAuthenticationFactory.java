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
package net.bluemind.authentication.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.bluemind.authentication.api.incore.IInCoreAuthentication;
import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.authentication.provider.ILoginSessionValidator;
import net.bluemind.authentication.provider.ILoginValidationListener;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class InCoreAuthenticationFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IInCoreAuthentication> {

	private final List<IAuthProvider> authProviders;
	private final List<ILoginValidationListener> loginListeners;
	private final List<ILoginSessionValidator> sessionValidators;

	public InCoreAuthenticationFactory() {
		RunnableExtensionLoader<IAuthProvider> rel = new RunnableExtensionLoader<IAuthProvider>();
		this.authProviders = rel.loadExtensions("net.bluemind.authentication.provider", "authprovider", "auth_provider",
				"impl");
		// max prio will be first
		Collections.sort(authProviders, new Comparator<IAuthProvider>() {

			@Override
			public int compare(IAuthProvider o1, IAuthProvider o2) {
				return -Integer.compare(o1.priority(), o2.priority());
			}
		});
		RunnableExtensionLoader<ILoginValidationListener> rel2 = new RunnableExtensionLoader<ILoginValidationListener>();
		this.loginListeners = rel2.loadExtensions("net.bluemind.authentication.provider", "loginvalidation",
				"validation_listener", "impl");

		this.sessionValidators = new RunnableExtensionLoader<ILoginSessionValidator>().loadExtensions(
				"net.bluemind.authentication.provider", "sessionvalidator", "session-validator", "class");
	}

	@Override
	public Class<IInCoreAuthentication> factoryClass() {
		return IInCoreAuthentication.class;
	}

	@Override
	public IInCoreAuthentication instance(BmContext context, String... params) throws ServerFault {
		return new Authentication(context, authProviders, loginListeners, sessionValidators);
	}
}
