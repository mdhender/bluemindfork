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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.context.SecurityContext;

public class ServiceMethodInvocation implements RestServiceInvocation {

	private static Logger logger = LoggerFactory.getLogger(ServiceMethodInvocation.class);
	private final Method method;

	public ServiceMethodInvocation(Method method) {
		this.method = method;
	}

	@Override
	public void invoke(SecurityContext securityContext, Object instance, Object[] params,
			AsyncHandler<Object> responseHandler) {
		logger.debug("Invoking method {}.{} : params {}", instance, method, params);
		try {
			Object resp = method.invoke(instance, params);
			responseHandler.success(resp);
		} catch (InvocationTargetException e) {
			logger.debug("during call {} {}", instance, params, e.getCause());
			responseHandler.failure(e.getCause());
		} catch (IllegalAccessException | IllegalArgumentException e) {
			logger.error("during call {} {}", instance, params, e.getCause());
			responseHandler.failure(e);
		}

	}

}
