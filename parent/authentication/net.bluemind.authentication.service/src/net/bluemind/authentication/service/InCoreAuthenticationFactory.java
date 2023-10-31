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
package net.bluemind.authentication.service;

import java.util.Collections;
import java.util.List;

import net.bluemind.authentication.api.incore.IInCoreAuthentication;
import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.authentication.provider.ILoginSessionValidator;
import net.bluemind.authentication.provider.ILoginValidationListener;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.service.internal.AuditLogService;
import net.bluemind.core.container.service.internal.SecurityContextAuditLogService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class InCoreAuthenticationFactory
		implements ServerSideServiceProvider.IServerSideServiceFactory<IInCoreAuthentication> {

	private static final String PROVIDER_PLUGIN_ID = "net.bluemind.authentication.provider";

	private final List<IAuthProvider> authProviders;
	private final List<ILoginValidationListener> loginListeners;
	private final List<ILoginSessionValidator> sessionValidators;

	public InCoreAuthenticationFactory() {
		RunnableExtensionLoader<IAuthProvider> rel = new RunnableExtensionLoader<IAuthProvider>();
		this.authProviders = rel.loadExtensions(PROVIDER_PLUGIN_ID, "authprovider", "auth_provider", "impl");
		// max prio will be first
		Collections.sort(authProviders,
				(IAuthProvider o1, IAuthProvider o2) -> -Integer.compare(o1.priority(), o2.priority()));
		RunnableExtensionLoader<ILoginValidationListener> rel2 = new RunnableExtensionLoader<ILoginValidationListener>();
		this.loginListeners = rel2.loadExtensions(PROVIDER_PLUGIN_ID, "loginvalidation", "validation_listener", "impl");

		this.sessionValidators = new RunnableExtensionLoader<ILoginSessionValidator>()
				.loadExtensions(PROVIDER_PLUGIN_ID, "sessionvalidator", "session-validator", "class");
	}

	@Override
	public Class<IInCoreAuthentication> factoryClass() {
		return IInCoreAuthentication.class;
	}

	@Override
	public IInCoreAuthentication instance(BmContext context, String... params) throws ServerFault {
		LoginAuditLogMapper mapper = new LoginAuditLogMapper();
		AuditLogService<SecurityContext, SecurityContext> auditLogService = new SecurityContextAuditLogService("login",
				mapper);
		return new Authentication(context, authProviders, loginListeners, sessionValidators, auditLogService);
	}
}
