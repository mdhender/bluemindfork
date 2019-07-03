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
import java.util.Map;

public class DefaultPathParameterCodecs {

	private static final List<PathParameterCodec.Factory<?>> codecs = new ArrayList<>();

	static {
		codecs.add(new SimpleFactory<>(String.class, new StringPathParameterCodec()));
		codecs.add(new SimpleFactory<>(Integer.class, new IntegerQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(int.class, new IntegerQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(Long.class, new LongQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(long.class, new LongQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(Boolean.class, new BooleanQueryParameterCodec()));
		codecs.add(new SimpleFactory<>(boolean.class, new BooleanQueryParameterCodec()));
		codecs.add(new EnumFactory<>());
	}

	private DefaultPathParameterCodecs() {
	}

	@SuppressWarnings("unchecked")
	public static <T> PathParameterCodec<T> factory(Class<T> parameterType) {
		PathParameterCodec<?> ret = null;
		for (PathParameterCodec.Factory<?> codec : codecs) {
			ret = codec.create(parameterType);
			if (ret != null) {
				break;
			}
		}
		return (PathParameterCodec<T>) ret;
	}

	public static final class SimpleFactory<T> implements PathParameterCodec.Factory<T> {
		private Class<T> parameterType;
		private PathParameterCodec<T> codec;

		public SimpleFactory(Class<T> parameterType, PathParameterCodec<T> codec) {
			this.parameterType = parameterType;
			this.codec = codec;
		}

		@Override
		public PathParameterCodec<T> create(Class<?> parameterType) {
			if (parameterType == this.parameterType) {
				return codec;
			} else {

				return null;
			}
		}

	}

	public static class StringPathParameterCodec implements PathParameterCodec<String> {

		@Override
		public String parse(String parameterValue) {
			return parameterValue;
		}

		@Override
		public void encode(String object, Map<String, String> params, String name) {
			if (object != null) {
				params.put(name, object);
			}
		}

	}

	public static class BooleanQueryParameterCodec implements PathParameterCodec<Boolean> {

		@Override
		public Boolean parse(String parameterValue) {
			if (parameterValue == null) {
				return null;
			}
			return Boolean.parseBoolean(parameterValue);
		}

		@Override
		public void encode(Boolean object, Map<String, String> params, String name) {
			if (object != null) {
				params.put(name, object.toString());
			}
		}

	}

	public static class LongQueryParameterCodec implements PathParameterCodec<Long> {

		@Override
		public Long parse(String parameterValue) {
			if (parameterValue == null) {
				return null;
			}
			return Long.valueOf(parameterValue);
		}

		@Override
		public void encode(Long object, Map<String, String> params, String name) {
			if (object != null) {
				params.put(name, object.toString());
			}
		}

	}

	public static class IntegerQueryParameterCodec implements PathParameterCodec<Integer> {

		@Override
		public Integer parse(String parameterValue) {
			if (parameterValue == null) {
				return null;
			}
			return Integer.valueOf(parameterValue);
		}

		@Override
		public void encode(Integer object, Map<String, String> params, String name) {
			if (object != null) {
				params.put(name, object.toString());
			}
		}

	}

	public static class EnumQueryParameterCodec<T extends Enum<T>> implements PathParameterCodec<T> {

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
		public void encode(T object, Map<String, String> params, String name) {
			if (object != null) {
				params.put(name, object.toString());
			}
		}

	}

	public static final class EnumFactory<T extends Enum<T>> implements PathParameterCodec.Factory<T> {

		@SuppressWarnings("unchecked")
		@Override
		public PathParameterCodec<T> create(Class<?> parameterType) {
			if (parameterType.isEnum()) {
				return new EnumQueryParameterCodec<>((Class<T>) parameterType);
			} else {
				return null;
			}
		}

	}
}
