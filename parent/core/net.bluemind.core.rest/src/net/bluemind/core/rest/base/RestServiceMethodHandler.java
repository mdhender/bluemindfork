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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpHeaders;

import io.netty.handler.codec.http.QueryStringDecoder;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.filter.IRestFilter;
import net.bluemind.core.rest.model.Endpoint;
import net.bluemind.core.rest.model.RestService;
import net.bluemind.core.rest.model.RestServiceApiDescriptor.MethodDescriptor;
import net.bluemind.core.rest.utils.ErrorLogBuilder;
import net.bluemind.core.sessions.Sessions;

public class RestServiceMethodHandler implements IRestCallHandler {

	private static final Logger logger = LoggerFactory.getLogger(RestServiceMethodHandler.class);
	private final Pattern pathRegexp;
	private final List<String> pathParamNames;
	private final RestServiceInvocation serviceInvocation;
	private final Endpoint endpoint;

	private final ParameterBuilder<? extends Object>[] paramBuilders;
	private final ResponseBuilder responseBuilder;
	private final List<IRestFilter> filters;
	private final String name;
	private static final Pattern pathParamsMatcher = Pattern.compile(Pattern.quote("{") + "(.*?)" + Pattern.quote("}"));

	private static final CharSequence X_BM_API_KEY = HttpHeaders.createOptimized("X-BM-ApiKey");

	public RestServiceMethodHandler(Endpoint endpoint, MethodDescriptor methodDescriptor, List<String> pathParamNames,
			ParameterBuilder<? extends Object>[] parameterBuilders, Pattern pathRegexp, ResponseBuilder responseBuilder,
			List<IRestFilter> filters) {
		this.endpoint = endpoint;
		this.paramBuilders = parameterBuilders;
		this.pathParamNames = pathParamNames;
		this.pathRegexp = pathRegexp;
		this.responseBuilder = responseBuilder;
		this.serviceInvocation = new RestServiceSecurityCheck(
				Collections.unmodifiableList(Arrays.asList(methodDescriptor.roles)),
				new ServiceMethodInvocation(methodDescriptor.interfaceMethod));
		this.filters = filters;
		this.name = Optional.ofNullable(methodDescriptor.httpMethodName).orElse("UNKNOWN") + "-"
				+ methodDescriptor.path;
	}

	@Override
	public String name() {
		return name;
	}

	public static RestServiceMethodHandler getInstance(RestService service, MethodDescriptor methodDescriptor,
			List<IRestFilter> filters) {
		Method method = methodDescriptor.interfaceMethod;

		Matcher matcher = pathParamsMatcher.matcher(methodDescriptor.path);
		List<String> pathParamNames = new ArrayList<>();
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String group = matcher.group().substring(1);
			if (pathParamNames.contains(group)) {
				throw new IllegalArgumentException(
						"Cannot use identifier " + group + " more than once in pattern string");
			}
			pathParamNames.add(group.substring(0, group.length() - 1));
			matcher.appendReplacement(sb, "(?<$1>[^\\/]+)");
		}
		matcher.appendTail(sb);
		Pattern pathRegexp = Pattern.compile(sb.toString());

		ParameterBuilder<? extends Object>[] parameterBuilders = new ParameterBuilder[method
				.getParameterTypes().length];
		int parameterIndex = 0;
		for (Parameter param : method.getParameters()) {
			ParameterBuilder<? extends Object> parameterBuilder = ParameterBuilder.getParameterBuilder(
					service.descriptor.apiInterface, method, param, method.getGenericParameterTypes()[parameterIndex]);
			parameterBuilders[parameterIndex] = parameterBuilder;
			parameterIndex++;
		}

		ResponseBuilder responseBuilder =

				ResponseBuilder.getResponseBuilder(methodDescriptor);
		return new RestServiceMethodHandler(service.endpoint, methodDescriptor, pathParamNames, parameterBuilders,
				pathRegexp, responseBuilder, filters);
	}

	@Override
	public void call(RestRequest request, AsyncHandler<RestResponse> response) {
		SecurityContext securityContext = null;
		String key = request.headers.get(X_BM_API_KEY);
		if (key == null) {
			key = request.params.get("apikey");
		}

		logger.debug("handle request {} from {}) with key {}", request.path, request.remoteAddresses, key);

		if (key == null) {
			securityContext = SecurityContext.ANONYMOUS.from(request.remoteAddresses);
		} else {

			securityContext = Sessions.sessionContext(key);

			if (securityContext == null) {
				response.success(RestResponse.invalidSession(String.format("session id %s is not valid", key)));
				return;
			}

			securityContext = securityContext.from(request.remoteAddresses);
		}

		for (IRestFilter filter : filters) {
			response = filter.authorized(request, securityContext, response);
			if (response == null) {
				return;
			}
		}

		logger.debug("[{} c:{}] handling {}", securityContext.getSubject(), securityContext.getContainerUid(),
				request.path);
		try {
			handle(securityContext, request, response);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.error("Error during restcall {}", request, e);
			} else {
				logger.error("Error during restcall {}:{}", ErrorLogBuilder.filter(request), ErrorLogBuilder.build(e));
			}
			response.success(responseBuilder.buildFailure(request, e));
		}

	}

	protected void handle(final SecurityContext securityContext, final RestRequest request,
			final AsyncHandler<RestResponse> response) throws ServerFault {
		RestInvocation invocation = buildInvocation(request);
		invocation.invoke(endpoint, securityContext, new AsyncHandler<Object>() {

			@Override
			public void success(Object value) {
				RestResponse responseMsg;
				try {
					responseMsg = responseBuilder.buildSuccess(request, value);
				} catch (Exception e) {
					failure(e);
					return;
				}
				response.success(responseMsg);
			}

			@Override
			public void failure(Throwable e) {
				if (logger.isDebugEnabled()) {
					logger.error("Error during restcall {}", request, e);
				} else {
					logger.error("Error during restcall {}:{}", ErrorLogBuilder.filter(request),
							ErrorLogBuilder.build(e));
				}
				response.success(responseBuilder.buildFailure(request, e));
			}
		});

	}

	private RestInvocation buildInvocation(RestRequest request) throws ServerFault {

		Matcher m = pathRegexp.matcher(request.path);
		m.matches();
		Map<String, String> pathParams = new HashMap<>();
		int len = pathParamNames.size();
		String[] path = new String[len];
		for (int i = 0; i < len; i++) {
			String paramName = pathParamNames.get(i);
			String paramVale = QueryStringDecoder.decodeComponent(m.group(paramName), StandardCharsets.UTF_8);
			pathParams.put(paramName, paramVale);
			path[i] = paramVale;
		}
		Object[] callArgs = new Object[paramBuilders.length];
		for (int i = 0; i < paramBuilders.length; i++) {
			try {
				callArgs[i] = paramBuilders[i].build(request, pathParams);
			} catch (Exception e) {
				logger.error("error during building params", e);
				throw new ServerFault("Error during parsing parameter \"" + paramBuilders[i].getParamName() + "\"");
			}
		}

		return new RestInvocation(path, callArgs);
	}

	private class RestInvocation {

		public final String[] pathValues;
		public final Object[] callArgs;

		public RestInvocation(String[] path, Object[] callArgs) {
			this.pathValues = path;
			this.callArgs = callArgs;
		}

		public void invoke(Endpoint endpoint, SecurityContext securityContext, AsyncHandler<Object> asyncHandler) {
			Object instance = null;
			try {
				instance = endpoint.getInstance(securityContext, pathValues);
			} catch (Exception e) {
				logger.error("during call: {}", ErrorLogBuilder.build(e));
				asyncHandler.failure(e);
				return;
			}
			serviceInvocation.invoke(securityContext, instance, callArgs, asyncHandler);
		}

	}

}
