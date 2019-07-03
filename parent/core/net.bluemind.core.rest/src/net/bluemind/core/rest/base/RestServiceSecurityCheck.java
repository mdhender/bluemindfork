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
package net.bluemind.core.rest.base;

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;

public class RestServiceSecurityCheck implements RestServiceInvocation {
	private final RestServiceInvocation nextHandler;
	private final List<String> requiredRoles;

	public RestServiceSecurityCheck(List<String> requiredRoles, RestServiceInvocation nextHandler) {
		this.requiredRoles = requiredRoles;
		this.nextHandler = nextHandler;
	}

	@Override
	public void invoke(SecurityContext securityContext, Object instance, Object[] params,
			AsyncHandler<Object> responseHandler) {

		if (!requiredRoles.isEmpty()) {
			boolean userOwnsNoRequiredRole = Collections.disjoint(securityContext.getRoles(), requiredRoles);
			if (userOwnsNoRequiredRole) {
				executeFaultHandler(responseHandler);
				return;
			}
		}

		invokeNext(securityContext, instance, params, responseHandler);
	}

	public void executeFaultHandler(AsyncHandler<Object> responseHandler) {
		responseHandler.failure(
				new ServerFault("API method call requires following roles: " + requiredRoles, ErrorCode.FORBIDDEN));
	}

	public void invokeNext(SecurityContext securityContext, Object instance, Object[] params,
			AsyncHandler<Object> responseHandler) {
		nextHandler.invoke(securityContext, instance, params, responseHandler);
	}

}
