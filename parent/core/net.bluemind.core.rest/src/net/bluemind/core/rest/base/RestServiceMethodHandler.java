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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.vertx.contextlogging.ContextualData;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.filter.IRestFilter;
import net.bluemind.core.rest.model.Endpoint;
import net.bluemind.core.rest.model.RestService;
import net.bluemind.core.rest.model.RestServiceApiDescriptor.MethodDescriptor;
import net.bluemind.core.rest.utils.ErrorLogBuilder;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.openid.utils.AccessTokenValidator;

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
	private final boolean async;
	private static final Pattern pathParamsMatcher = Pattern.compile(Pattern.quote("{") + "(.*?)" + Pattern.quote("}"));
	private static final CharSequence X_BM_API_KEY = HttpHeaders.createOptimized("X-BM-ApiKey");
	private static final ServerCookieDecoder cookieDecoder = ServerCookieDecoder.LAX;
	private static final String OPENID_COOKIE = "OpenIdSession";
	private static final String ACCESS_COOKIE = "AccessToken";
	private static final String REFRESH_COOKIE = "RefreshToken";
	private static final String ID_COOKIE = "IdToken";

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
				new ServiceMethodInvocation(methodDescriptor.interfaceMethod, methodDescriptor.async));
		this.filters = filters;
		this.name = Optional.ofNullable(methodDescriptor.httpMethodName).orElse("UNKNOWN") + "-"
				+ methodDescriptor.path;
		this.async = methodDescriptor.async;
	}

	@Override
	public String name() {
		return name;
	}

	private static final Map<String, Pattern> patternsCache = new ConcurrentHashMap<>();

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
		Pattern pathRegexp = patternsCache.computeIfAbsent(sb.toString(), Pattern::compile);

		ParameterBuilder<? extends Object>[] parameterBuilders = new ParameterBuilder<?>[method
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
		String key = null;
		String cookieStr = Optional.ofNullable(request.headers.get("cookie")).orElse("");
		Set<Cookie> cookies = cookieDecoder.decode(cookieStr);
		Optional<Cookie> oidc = cookies.stream().filter(c -> OPENID_COOKIE.equals(c.name())).findFirst();
		MultiMap headers = MultiMap.caseInsensitiveMultiMap();

		if (oidc.isPresent()) {
			JsonObject token = new JsonObject(oidc.get().value());
			key = token.getString("sid");
			String domainUid = token.getString("domain_uid");

			Optional<Cookie> atc = cookies.stream().filter(c -> ACCESS_COOKIE.equals(c.name())).findFirst();
			if (atc.isEmpty()) {
				error(response, key, new ServerFault("No access token cookie"));
				return;
			}
			DecodedJWT accessToken = JWT.decode(atc.get().value());
			try {
				AccessTokenValidator.validateSignature(domainUid, accessToken);
			} catch (ServerFault sf) {
				error(response, key, sf);
				return;
			}
			try {
				AccessTokenValidator.validate(domainUid, accessToken);
			} catch (ServerFault sf) {
				Optional<Cookie> rtc = cookies.stream().filter(c -> REFRESH_COOKIE.equals(c.name())).findFirst();
				if (rtc.isEmpty()) {
					error(response, key, new ServerFault("No refresh token cookie"));
					return;
				}
				CompletableFuture<Optional<JsonObject>> future = AccessTokenValidator.refreshToken(domainUid,
						rtc.get().value());
				Optional<JsonObject> refreshedToken;
				try {
					refreshedToken = future.get(10, TimeUnit.SECONDS);
				} catch (Exception e) {
					error(response, key, e);
					return;
				}

				if (refreshedToken.isEmpty()) {
					error(response, key, sf);
					return;
				}

				JsonObject rt = new JsonObject(refreshedToken.get().encode());
				JsonObject cookie = new JsonObject();
				cookie.put("sid", key);
				cookie.put("domain_uid", domainUid);

				Cookie openIdCookie = new DefaultCookie(OPENID_COOKIE, cookie.encode());
				openIdCookie.setPath("/");
				openIdCookie.setHttpOnly(true);
				openIdCookie.setSecure(true);
				headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(openIdCookie));

				Cookie accessCookie = new DefaultCookie(ACCESS_COOKIE, rt.getString("access_token"));
				accessCookie.setPath("/");
				accessCookie.setHttpOnly(true);
				accessCookie.setSecure(true);
				headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(accessCookie));

				String refreshToken = rtc.get().value();
				if (rt.containsKey("refresh_token")) {
					refreshToken = rt.getString("refresh_token");
				}

				Cookie refreshCookie = new DefaultCookie(REFRESH_COOKIE, refreshToken);
				refreshCookie.setPath("/");
				refreshCookie.setHttpOnly(true);
				refreshCookie.setSecure(true);
				headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(refreshCookie));

				Cookie idCookie = new DefaultCookie(ID_COOKIE, rt.getString("id_token"));
				idCookie.setPath("/");
				idCookie.setHttpOnly(true);
				idCookie.setSecure(true);
				headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(idCookie));
			}
		}

		key = Optional.ofNullable(key)
				.orElse(Optional.ofNullable(request.headers.get(X_BM_API_KEY)).orElse(request.params.get("apikey")));

		logger.debug("handle request {} from {}) with key {}", request.path, request.remoteAddresses, key);

		SecurityContext securityContext = null;
		if (key == null) {
			securityContext = SecurityContext.ANONYMOUS.from(request.remoteAddresses, null);
		} else {
			securityContext = Sessions.sessionContext(key);
			if (securityContext == null) {
				String msg = "session id " + key + " is not valid";
				error(response, key, new ServerFault(msg));
				response.success(RestResponse.invalidSession(msg));
				return;
			}
			securityContext = securityContext.from(request.remoteAddresses,
					request.headers.get(RestHeaders.X_BM_ORIGIN));
			ContextualData.put("user", securityContext.getSubjectDisplayName());
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
			handle(securityContext, request, response, headers);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.error("Error during restcall {}", request, e);
			} else {
				logger.error("Error during restcall {}:{}", ErrorLogBuilder.filter(request), ErrorLogBuilder.build(e));
			}
			response.success(responseBuilder.buildFailure(request, e));
		}

	}

	private void error(AsyncHandler<RestResponse> response, String key, Exception e) {
		if (logger.isDebugEnabled()) {
			logger.error("Failed to validate AccessToken: ", e);
		} else {
			logger.error(e.getMessage());
		}
		Sessions.get().invalidate(key);
		RestResponse resp = RestResponse.invalidSession(String.format("invalid accesstoken: %s", e.getMessage()));
		resp.headers.add("WWW-authenticate", "Bearer");

		Cookie openIdCookie = new DefaultCookie(OPENID_COOKIE, "");
		openIdCookie.setPath("/");
		openIdCookie.setMaxAge(0);
		openIdCookie.setHttpOnly(true);
		openIdCookie.setSecure(true);
		resp.headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(openIdCookie));

		Cookie accessCookie = new DefaultCookie(ACCESS_COOKIE, "");
		accessCookie.setPath("/");
		accessCookie.setMaxAge(0);
		accessCookie.setHttpOnly(true);
		accessCookie.setSecure(true);
		resp.headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(accessCookie));

		Cookie refreshCookie = new DefaultCookie(REFRESH_COOKIE, "");
		refreshCookie.setPath("/");
		refreshCookie.setMaxAge(0);
		refreshCookie.setHttpOnly(true);
		refreshCookie.setSecure(true);
		resp.headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(refreshCookie));

		Cookie idCookie = new DefaultCookie(ID_COOKIE, "");
		idCookie.setPath("/");
		idCookie.setMaxAge(0);
		idCookie.setHttpOnly(true);
		idCookie.setSecure(true);
		resp.headers.add(HttpHeaders.SET_COOKIE, ServerCookieEncoder.LAX.encode(idCookie));

		response.success(resp);
	}

	private void handle(final SecurityContext securityContext, final RestRequest request,
			final AsyncHandler<RestResponse> response, MultiMap headers) {
		RestInvocation invocation = buildInvocation(request);
		invocation.invoke(endpoint, securityContext, new AsyncHandler<Object>() {

			@Override
			public void success(Object value) {
				if (async) {
					CompletableFuture<?> ret = (CompletableFuture<?>) value;
					ret.thenAccept(val -> createResponse(request, response, val, headers)).exceptionally(e -> {
						failure(e);
						return null;
					});
				} else {
					createResponse(request, response, value, headers);
				}
			}

			private void createResponse(final RestRequest request, final AsyncHandler<RestResponse> response,
					Object value, MultiMap headers) {
				RestResponse responseMsg;
				try {
					responseMsg = responseBuilder.buildSuccess(request, value);
				} catch (Exception e) {
					failure(e);
					return;
				}
				responseMsg.headers.addAll(headers);
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

	private RestInvocation buildInvocation(RestRequest request) {

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
