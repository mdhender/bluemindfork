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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;

import net.bluemind.core.rest.base.codec.BodyParameterCodec;
import net.bluemind.core.rest.base.codec.DefaultBodyParameterCodecs;
import net.bluemind.core.rest.base.codec.DefaultPathParameterCodecs;
import net.bluemind.core.rest.base.codec.DefaultQueryParameterCodecs;
import net.bluemind.core.rest.base.codec.PathParameterCodec;
import net.bluemind.core.rest.base.codec.QueryParameterCodec;

public abstract class ParameterBuilder<T> {

	static final Logger logger = LoggerFactory.getLogger(ParameterBuilder.class);

	public abstract String getParamName();

	public abstract T build(RestRequest request, Map<String, String> pathParams) throws Exception;

	public static ParameterBuilder<? extends Object> getParameterBuilder(Class<?> apiInterface, Method method,
			Parameter param, Type genericParamType) {
		for (Annotation annotation : param.getAnnotations()) {
			if (annotation.annotationType() == PathParam.class) {
				PathParam pp = (PathParam) annotation;
				return new PathParameterBuilder(pp.value(), param.getType());
			}

			if (annotation.annotationType() == QueryParam.class) {
				QueryParam queryParam = (QueryParam) annotation;
				return new QueryParameterBuilder(queryParam.value(), param.getType());
			}
		}

		if (genericParamType instanceof TypeVariable) {
			TypeToken<?> type = TypeToken.of(apiInterface).resolveType(genericParamType);
			return new BodyParameterBuilder((Class) type.getRawType(), type.getType());
		} else {
			return new BodyParameterBuilder(param.getType(), param.getParameterizedType());
		}

	}

	private static class PathParameterBuilder<T> extends ParameterBuilder<T> {
		private final String pathParameter;
		private PathParameterCodec<T> codec;

		private PathParameterBuilder(String pathParameter, Class<T> paramType) {
			this.pathParameter = pathParameter;
			codec = DefaultPathParameterCodecs.factory(paramType);
			if (codec == null) {
				throw new IllegalArgumentException("Unsupported type " + paramType);
			}
		}

		@Override
		public T build(RestRequest request, Map<String, String> pathParams) throws Exception {
			String value = pathParams.get(pathParameter);
			if (value != null) {
				return codec.parse(value);
			} else {
				return null;
			}
		}

		@Override
		public String getParamName() {
			return pathParameter;
		}

	}

	private static class QueryParameterBuilder<T> extends ParameterBuilder<T> {
		private final String pathParameter;
		private QueryParameterCodec<T> codec;

		private QueryParameterBuilder(String pathParameter, Class<T> parameterType) {
			this.pathParameter = pathParameter;
			codec = DefaultQueryParameterCodecs.factory(parameterType);
			if (codec == null) {
				throw new IllegalArgumentException("Unsupported type " + parameterType);
			}
		}

		@Override
		public T build(RestRequest request, Map<String, String> pathParams) throws Exception {

			String value = request.params.get(pathParameter);
			return codec.parse(value);

		}

		@Override
		public String getParamName() {
			return pathParameter;
		}
	}

	private static class BodyParameterBuilder<T> extends ParameterBuilder<T> {
		private BodyParameterCodec<?> codec;

		private BodyParameterBuilder(Class<T> klass, Type type) {
			codec = DefaultBodyParameterCodecs.factory((Class<?>) klass, type);
		}

		@Override
		public T build(RestRequest request, Map<String, String> pathParams) throws Exception {
			return (T) codec.parse(request);
		}

		@Override
		public String getParamName() {
			return "body";
		}

	}
}
