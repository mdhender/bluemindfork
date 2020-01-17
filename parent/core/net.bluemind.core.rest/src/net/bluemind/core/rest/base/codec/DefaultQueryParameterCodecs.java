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
package net.bluemind.core.rest.base.codec;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.MultiMap;
import net.bluemind.core.rest.base.codec.QueryParameterCodec.Factory;

public class DefaultQueryParameterCodecs {

	private static final List<QueryParameterCodec.Factory<?>> codecs = new ArrayList<>();

	static {
		codecs.add(new SimpleFactory<>(String.class, new StringQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(Integer.class, new IntegerQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(int.class, new IntegerQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(Long.class, new LongQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(long.class, new LongQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(Boolean.class, new BooleanQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(boolean.class, new BooleanQueryParameterCodec()));
		codecs.add(new EnumFactory<>());
	}

	@SuppressWarnings("unchecked")
	public static <T> QueryParameterCodec<T> factory(Class<T> parameterType) {
		QueryParameterCodec<?> ret = null;
		for (Factory<?> codec : codecs) {
			ret = codec.create(parameterType);
			if (ret != null) {
				break;
			}
		}
		return (QueryParameterCodec<T>) ret;
	}

	public static final class SimpleFactory<T> implements QueryParameterCodec.Factory<T> {
		private Class<T> parameterType;
		private QueryParameterCodec<T> codec;

		public SimpleFactory(Class<T> parameterType, QueryParameterCodec<T> codec) {
			this.parameterType = parameterType;
			this.codec = codec;
		}

		@Override
		public QueryParameterCodec<T> create(Class<?> parameterType) {
			if (parameterType == this.parameterType) {
				return codec;
			} else {

				return null;
			}
		}

	}

	public static class StringQueryParameterCodec implements QueryParameterCodec<String> {

		@Override
		public String parse(String parameterValue) {
			return parameterValue;
		}

		@Override
		public void encode(String object, MultiMap params, String name) {
			if (object != null) {
				params.add(name, object);
			}
		}

	}

	public static class BooleanQueryParameterCodec implements QueryParameterCodec<Boolean> {

		@Override
		public Boolean parse(String parameterValue) {
			if (parameterValue == null) {
				return null;
			}
			return Boolean.parseBoolean(parameterValue);
		}

		@Override
		public void encode(Boolean object, MultiMap params, String name) {
			if (object != null) {
				params.add(name, object.toString());
			}
		}

	}

	public static class LongQueryParameterCodec implements QueryParameterCodec<Long> {

		@Override
		public Long parse(String parameterValue) {
			if (parameterValue == null) {
				return null;
			}
			return Long.parseLong(parameterValue);
		}

		@Override
		public void encode(Long object, MultiMap params, String name) {
			if (object != null) {
				params.add(name, object.toString());
			}
		}

	}

	public static class IntegerQueryParameterCodec implements QueryParameterCodec<Integer> {

		@Override
		public Integer parse(String parameterValue) {
			if (parameterValue == null) {
				return null;
			}
			return Integer.parseInt(parameterValue);
		}

		@Override
		public void encode(Integer object, MultiMap params, String name) {
			if (object != null) {
				params.add(name, object.toString());
			}
		}

	}

	public static class EnumQueryParameterCodec<T extends Enum<T>> implements QueryParameterCodec<T> {

		private Class<T> parametertType;

		public EnumQueryParameterCodec(Class<T> parametertType) {
			this.parametertType = parametertType;
		}

		@Override
		public T parse(String parameterValue) {
			if (parameterValue == null) {
				return null;
			}
			return (T) Enum.valueOf(parametertType, parameterValue);
		}

		@Override
		public void encode(T object, MultiMap params, String name) {
			if (object != null) {
				params.add(name, object.toString());
			}
		}

	}

	public static final class EnumFactory<T extends Enum<T>> implements QueryParameterCodec.Factory<T> {

		@SuppressWarnings("unchecked")
		@Override
		public QueryParameterCodec<T> create(Class<?> parameterType) {
			if (parameterType.isEnum()) {
				return new EnumQueryParameterCodec<>((Class<T>) parameterType);
			} else {
				return null;
			}
		}

	}
}
