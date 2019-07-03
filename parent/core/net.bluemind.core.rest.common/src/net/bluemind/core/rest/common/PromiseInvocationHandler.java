/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.core.rest.common;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.AsyncHandler;

public class PromiseInvocationHandler implements InvocationHandler {

	private static final Logger logger = LoggerFactory.getLogger(PromiseInvocationHandler.class);
	private final Object asyncImpl;

	public PromiseInvocationHandler(Object asyncImpl) {
		this.asyncImpl = asyncImpl;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
		Object[] args = arguments == null ? new Object[0] : arguments;
		Class<?>[] paramTypes = method.getParameterTypes();
		Class<?>[] paramTypesWithAsyncHandler = new Class<?>[paramTypes.length + 1];
		Object[] argsWithAsyncHandler = new Object[args.length + 1];
		int i;
		for (i = 0; i < args.length; i++) {
			argsWithAsyncHandler[i] = args[i];
			paramTypesWithAsyncHandler[i] = paramTypes[i];
		}
		paramTypesWithAsyncHandler[i] = AsyncHandler.class;
		CompletableFuture<Object> promise = new CompletableFuture<Object>();
		net.bluemind.core.api.AsyncHandler<Object> anyTypeHandle = new net.bluemind.core.api.AsyncHandler<Object>() {

			@Override
			public void success(Object value) {
				if (logger.isDebugEnabled()) {
					logger.debug("Success for promise with value {}", value);
				}
				promise.complete(value);
			}

			@Override
			public void failure(Throwable e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failure for promise with throwable", e);
				}
				promise.completeExceptionally(e);
			}
		};
		argsWithAsyncHandler[i] = anyTypeHandle;
		Method asyncEquivalent = asyncImpl.getClass().getMethod(method.getName(), paramTypesWithAsyncHandler);
		asyncEquivalent.invoke(asyncImpl, argsWithAsyncHandler);
		return promise;
	}

}