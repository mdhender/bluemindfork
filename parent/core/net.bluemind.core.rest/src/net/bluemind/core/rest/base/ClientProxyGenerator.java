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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.RestServiceApiDescriptorBuilder;
import net.bluemind.core.rest.base.codec.BodyParameterCodec;
import net.bluemind.core.rest.base.codec.DefaultBodyParameterCodecs;
import net.bluemind.core.rest.base.codec.DefaultQueryParameterCodecs;
import net.bluemind.core.rest.base.codec.DefaultResponseCodecs;
import net.bluemind.core.rest.base.codec.QueryParameterCodec;
import net.bluemind.core.rest.base.codec.ResponseCodec;
import net.bluemind.core.rest.model.RestServiceApiDescriptor;
import net.bluemind.core.rest.model.RestServiceApiDescriptor.MethodDescriptor;

public class ClientProxyGenerator<S, T> {

	public static final Logger logger = LoggerFactory.getLogger(ClientProxyGenerator.class);
	final Class<S> api;
	final Class<T> asyncApi;

	List<PathComponent> rootPathComponents;
	Map<String, EventMethodInvoker> methodsMap = new HashMap<>();

	static final String URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS = //
			"-._~" + // Unreserved characters.
					"!$'()*,;&=" + // The subdelim characters (excluding
									// '+').
					"@:"; // The gendelim characters permitted in paths.

	private static final Escaper URL_PATH_SEGMENT_ESCAPER = new PercentEscaper(URL_PATH_OTHER_SAFE_CHARS_LACKING_PLUS,
			true);

	public ClientProxyGenerator(Class<S> api, Class<T> asyncApi) {
		this.api = api;
		this.asyncApi = asyncApi;
		RestServiceApiDescriptor descr = new RestServiceApiDescriptorBuilder().build(api);

		this.rootPathComponents = parsePathCompenents(descr.rootPath);
		for (Method m : api.getMethods()) {
			for (MethodDescriptor method : descr.methods) {
				if (method.getApiInterfaceName().equals(m.getName())) {
					List<Parameter> params = new ArrayList<>();
					for (int i = 0; i < method.interfaceMethod.getParameterAnnotations().length; i++) {
						Annotation[] k = method.interfaceMethod.getParameterAnnotations()[i];
						boolean pathParam = false;
						boolean queryParam = false;
						for (Annotation a : k) {
							if (a.annotationType() == PathParam.class) {
								PathParam p = PathParam.class.cast(a);
								params.add(new PathParameter(i, p.value()));
								pathParam = true;
							}

							if (a.annotationType() == QueryParam.class) {
								QueryParam p = QueryParam.class.cast(a);

								QueryParameterCodec<?> codec = DefaultQueryParameterCodecs
										.factory(method.interfaceMethod.getParameters()[i].getType());
								params.add(new QueryParameter<>(i, p.value(), codec));
								queryParam = true;
							}
						}

						if (!pathParam && !queryParam) {
							BodyParameterCodec<?> codec = DefaultBodyParameterCodecs.factory(
									method.interfaceMethod.getParameters()[i].getType(),
									method.interfaceMethod.getParameters()[i].getParameterizedType());

							params.add(new JsonParameter(i, codec));
						}
					}

					List<PathComponent> pathComponents = parsePathCompenents(method.path);
					PatternBinding binding = new PatternBinding(pathComponents);

					ResponseCodec<?> retCodec = DefaultResponseCodecs
							.codec(method.interfaceMethod.getGenericReturnType(), method.produces[0]);

					if (method.interfaceMethod.getGenericReturnType().getTypeName()
							.equals("net.bluemind.core.container.model.ItemValue<T>")) {
						if (method.genericType != null) {
							retCodec = DefaultResponseCodecs.codec(method.genericType, method.produces[0]);
						}
					}

					MethodCallBuilder callBuilder = new MethodCallBuilder(method.httpMethodName, params, binding,
							method.produces, retCodec);

					this.methodsMap.put(method.getApiInterfaceName(), new EventMethodInvoker(callBuilder));
					break;
				}
			}
		}
	}

	static final class PatternBinding {

		private List<PathComponent> pathComponents;

		public PatternBinding(List<PathComponent> pathComponents) {
			this.pathComponents = pathComponents;
		}

		public String build(Map<String, String> params) throws ServerFault {
			StringBuilder sb = new StringBuilder();
			for (PathComponent c : pathComponents) {
				c.append(sb, params);
			}

			return sb.toString();
		}
	}

	private List<PathComponent> parsePathCompenents(String path) {
		List<PathComponent> pathComponents = new ArrayList<>();
		path = path.replace("{", ":");
		path = path.replace("}", "");

		Matcher m = Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)").matcher(path);
		int last = 0;

		while (m.find()) {
			String group = m.group().substring(1);

			pathComponents.add(new PathComponent(path.substring(last, m.start()), false));
			pathComponents.add(new PathComponent(group, true));
			last = m.end();
		}

		if (last < path.length()) {
			pathComponents.add(new PathComponent(path.substring(last, path.length()), false));

		}
		return pathComponents;
	}

	static class PathComponent {
		String value;
		boolean substitute;

		public PathComponent(String value, boolean substitute) {
			this.value = value;
			this.substitute = substitute;
		}

		public void append(StringBuilder path, Map<String, String> pathParams) throws ServerFault {
			if (substitute) {
				String v = pathParams.get(value);
				if (v != null && v.length() > 0) {
					path.append(v);
				} else {
					throw new ServerFault("param " + value + " is null", ErrorCode.INVALID_PARAMETER);
				}
			} else {
				path.append(value);
			}
		}
	}

	public class EventMethodInvoker {
		private MethodCallBuilder callBuilder;

		public EventMethodInvoker(MethodCallBuilder callBuilder) {
			this.callBuilder = callBuilder;
		}

		public void invoke(String origin, List<String> remoteAddresses, IRestCallHandler callHandler,
				MultiMap defaultHeaders, String[] instanceParams, Object[] args,
				final AsyncHandler<Object> responseHandler) throws Exception {

			RestRequest request = callBuilder.build(instanceParams, args);
			request.headers.addAll(defaultHeaders);
			request.remoteAddresses = remoteAddresses;
			request.origin = origin;
			logger.debug("send request {}", request);
			callHandler.call(request, new AsyncHandler<RestResponse>() {

				@Override
				public void success(RestResponse value) {
					if (value.headers.contains("X-BM-WarnMessage")) {
						logger.warn("warn message in call response : {}", value.headers.getAll("X-BM-WarnMessage"));
					}

					try {
						responseHandler.success(callBuilder.parseResponse(value));
					} catch (ServerFault e) {
						responseHandler.failure(e);
					} catch (Exception e) {
						responseHandler.failure(new ServerFault(e));

					}

				}

				@Override
				public void failure(Throwable e) {
					responseHandler.failure(e);
				}

			});
		}

	}

	public T client(final String origin, final List<String> remoteIps, final IRestCallHandler callHandler,
			final MultiMap defaultHeaders, final String... params) {
		Object proxy = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { this.asyncApi },
				(Object prox, Method method, Object[] args) -> {
					EventMethodInvoker m = methodsMap.get(method.getName());
					@SuppressWarnings("unchecked")
					AsyncHandler<Object> respHandler = (AsyncHandler<Object>) args[args.length - 1];
					m.invoke(origin, remoteIps, callHandler, defaultHeaders, params, args, respHandler);
					return null;
				});

		return asyncApi.cast(proxy);
	}

	public S syncClient(final String origin, final List<String> remoteIps, final IRestCallHandler callHandler,
			final MultiMap defaultHeaders, final String... params) {
		Object proxy = Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[] { api }, new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				EventMethodInvoker m = methodsMap.get(method.getName());
				final CompletableFuture<Object> f = new CompletableFuture<>();

				m.invoke(origin, remoteIps, callHandler, defaultHeaders, params, args, new AsyncHandler<Object>() {

					@Override
					public void success(Object value) {
						f.complete(value);
					}

					@Override
					public void failure(Throwable e) {
						f.completeExceptionally(e);
					}
				});
				try {
					return f.get(5, TimeUnit.MINUTES);
				} catch (ExecutionException e) {
					Throwable t = e.getCause();
					if (t instanceof ServerFault) {
						ServerFault toThrow = new ServerFault(t.getMessage(), t);
						toThrow.setCode(((ServerFault) t).getCode());
						throw toThrow;
					} else {
						throw new ServerFault(t);
					}

				} catch (Exception e) {
					throw e;
				}
			}
		});

		return api.cast(proxy);
	}

	public Map<String, String> buildRootPathParams(String[] instanceParams) {

		Map<String, String> params = new HashMap<>();
		int iParamsPos = 0;
		for (PathComponent comp : rootPathComponents) {
			if (comp.substitute) {
				params.put(comp.value, URL_PATH_SEGMENT_ESCAPER.escape(instanceParams[iParamsPos]));
				iParamsPos++;
			}
		}
		return params;
	}

	static interface Parameter {

		public void setParameter(HttpRequestBuilder requestBuilder, Object[] args) throws Exception;
	}

	private static class PathParameter implements Parameter {
		private final int index;
		private String name;

		public PathParameter(int index, String name) {
			this.index = index;
			this.name = name;
		}

		@Override
		public void setParameter(HttpRequestBuilder requestBuilder, Object[] args) {
			if (args[index] != null) {
				requestBuilder.pathParams.put(name, URL_PATH_SEGMENT_ESCAPER.escape(args[index].toString()));
			}
		}

	}

	private class QueryParameter<Q> implements Parameter {
		private final int index;
		private String name;
		private QueryParameterCodec<Q> codec;

		public QueryParameter(int index, String name, QueryParameterCodec<Q> codec) {
			this.index = index;
			this.name = name;
			this.codec = codec;
		}

		@Override
		public void setParameter(HttpRequestBuilder requestBuilder, Object[] args) {
			codec.encode((Q) args[index], requestBuilder.request.params, name);
		}

	}

	private class JsonParameter<Q> implements Parameter {
		private final int index;
		private BodyParameterCodec<Q> codec;

		public JsonParameter(int index, BodyParameterCodec<Q> codec) {
			this.index = index;
			this.codec = codec;
		}

		@Override
		public void setParameter(final HttpRequestBuilder requestBuilder, Object[] args) throws Exception {
			codec.encode((Q) args[index], requestBuilder.request);
		}

	}

	private class MethodCallBuilder {

		private PatternBinding binding;
		private List<Parameter> params;
		private HttpMethod methodName;
		private String mimeType;
		private ResponseCodec<?> responseCodec;

		public MethodCallBuilder(String httpMethodName, List<Parameter> params, PatternBinding binding,
				String[] produces, ResponseCodec<?> responseCodec) {
			this.methodName = HttpMethod.valueOf(httpMethodName);
			this.params = params;
			this.binding = binding;
			this.mimeType = produces[0];
			this.responseCodec = responseCodec;
		}

		public RestRequest build(String[] instanceParams, Object[] args) throws Exception {
			MultiMap headers = RestHeaders.newMultimap();
			headers.add(HttpHeaders.ACCEPT, mimeType);
			MultiMap queryParams = MultiMap.caseInsensitiveMultiMap();

			RestRequest request = new RestRequest(null, null, methodName, headers, null, queryParams, null, null);
			HttpRequestBuilder builder = new HttpRequestBuilder();
			builder.request = request;

			Map<String, String> parameters = buildRootPathParams(instanceParams);
			builder.pathParams = parameters;
			for (Parameter p : params) {
				p.setParameter(builder, args);
			}
			String path = binding.build(builder.pathParams);

			request.path = path;
			return request;
		}

		public Object parseResponse(RestResponse resp) throws Exception {
			Object decode = responseCodec.decode(resp);
			return decode;
		}
	}

	public static class HttpRequestBuilder {
		public RestRequest request;
		Map<String, String> pathParams = new HashMap<>();
	}

	private static final Map<Class<?>, ClientProxyGenerator<?, ?>> cached = new ConcurrentHashMap<>();

	@SuppressWarnings("serial")
	private static class RestGeneratorException extends RuntimeException {
		public RestGeneratorException(Throwable t) {
			super(t);
		}
	}

	@SuppressWarnings("unchecked")
	public static <S, T> ClientProxyGenerator<S, T> generator(Class<S> api, Class<T> asyncApi) {
		ClientProxyGenerator<S, T> cachedGen = (ClientProxyGenerator<S, T>) cached.get(api);
		if (cachedGen != null) {
			return cachedGen;
		}
		if (asyncApi == null) {
			try {
				asyncApi = (Class<T>) Class.forName(api.getCanonicalName() + "Async", false, api.getClassLoader());
			} catch (ClassNotFoundException e) {
				throw new RestGeneratorException(e);
			}
		}

		cachedGen = new ClientProxyGenerator<>(api, asyncApi);
		cached.put(api, cachedGen);
		return cachedGen;
	}
}
