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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vertx.java.core.buffer.Buffer;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import io.netty.buffer.Unpooled;
import net.bluemind.core.api.Stream;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.vertx.VertxStream;

public class DefaultBodyParameterCodecs {

	private static final List<BodyParameterCodec.Factory<?>> codecs = new ArrayList<>();

	static {
		codecs.add(new StreamFactory());
		codecs.add(new StringFactory());
		codecs.add(new ByteArrayFactory());
		codecs.add(new ObjectFactory());
	}

	@SuppressWarnings("unchecked")
	public static <T> BodyParameterCodec<T> factory(Class<T> parameterType, Type type) {
		BodyParameterCodec<?> ret = null;
		for (BodyParameterCodec.Factory<?> codec : codecs) {
			ret = codec.create(parameterType, type);
			if (ret != null) {
				break;
			}
		}
		return (BodyParameterCodec<T>) ret;
	}

	public static class StreamBodyCodec implements BodyParameterCodec<Stream> {

		public static final StreamBodyCodec INSTANCE = new StreamBodyCodec();

		@Override
		public Stream parse(RestRequest request) {
			if (request.bodyStream == null) {
				return VertxStream.stream(request.body);
			} else {
				return VertxStream.stream(request.bodyStream);
			}
		}

		@Override
		public void encode(Stream object, RestRequest request) {
			request.body = null;
			request.bodyStream = VertxStream.read(object);
		}

	}

	public static class ByMimeTypeCodec<T> implements BodyParameterCodec<T> {

		private Map<String, BodyParameterCodec<T>> codecs;
		private String defaultMimeType;

		ByMimeTypeCodec(Map<String, BodyParameterCodec<T>> codecs, String defaultMimeType) {
			this.codecs = codecs;
			this.defaultMimeType = defaultMimeType;
		}

		@Override
		public T parse(RestRequest request) {
			String m = request.headers.get("Content-Type");
			if (Strings.isNullOrEmpty(m)) {
				m = defaultMimeType;
			}

			BodyParameterCodec<T> codec = codecs.get(m);
			if (codec == null) {
				codec = codecs.get(defaultMimeType);
			}

			return codec.parse(request);
		}

		@Override
		public void encode(T object, RestRequest request) {
			String m = request.headers.get("Content-Type");
			if (Strings.isNullOrEmpty(m)) {
				m = defaultMimeType;
			}

			BodyParameterCodec<T> codec = codecs.get(m);
			if (codec == null) {
				request.headers.add("Content-Type", defaultMimeType);
				codec = codecs.get(defaultMimeType);
			} else {
				request.headers.add("Content-Type", m);
			}

			codec.encode(object, request);
		}

	}

	public static class ObjectBodyCodec<T> implements BodyParameterCodec<T> {

		private ByMimeTypeCodec<T> byMimeType;

		ObjectBodyCodec(Type bodyType) {
			Map<String, BodyParameterCodec<T>> map = ImmutableMap.<String, BodyParameterCodec<T>>builder()
					.put("application/json", new JsonObjectCodec.Body<>(bodyType)).build();
			byMimeType = new ByMimeTypeCodec<>(map, "application/json");
		}

		@Override
		public T parse(RestRequest request) {
			return byMimeType.parse(request);
		}

		@Override
		public void encode(T object, RestRequest request) {
			byMimeType.encode(object, request);
		}

	}

	public static class PlainTextBodyCodec implements BodyParameterCodec<String> {

		@Override
		public String parse(RestRequest request) {
			return request.body.toString();
		}

		@Override
		public void encode(String object, RestRequest request) {
			request.body = new Buffer(object);
		}

	}

	public static class OctetStreamByeArrayBodyCodec implements BodyParameterCodec<byte[]> {

		@Override
		public byte[] parse(RestRequest request) {
			return request.body.getBytes();
		}

		@Override
		public void encode(byte[] object, RestRequest request) {
			request.body = new Buffer(Unpooled.wrappedBuffer(object));
		}

	}

	public static class StringBodyCodec implements BodyParameterCodec<String> {

		private ByMimeTypeCodec<String> byMimeType;

		StringBodyCodec() {
			Map<String, BodyParameterCodec<String>> map = ImmutableMap.<String, BodyParameterCodec<String>>builder()
					.put("application/json", new JsonObjectCodec.Body<>(String.class))
					.put("text/plain", new PlainTextBodyCodec()).build();
			byMimeType = new ByMimeTypeCodec<>(map, "application/json");
		}

		@Override
		public String parse(RestRequest request) {
			return byMimeType.parse(request);
		}

		@Override
		public void encode(String object, RestRequest request) {
			byMimeType.encode(object, request);
		}

	}

	public static class ByteArrayBodyCodec implements BodyParameterCodec<byte[]> {

		private ByMimeTypeCodec<byte[]> byMimeType;

		ByteArrayBodyCodec() {
			Map<String, BodyParameterCodec<byte[]>> map = ImmutableMap.<String, BodyParameterCodec<byte[]>>builder()
					.put("application/json", new JsonObjectCodec.Body<>(byte[].class))
					.put("application/octet-stream", new OctetStreamByeArrayBodyCodec()).build();
			byMimeType = new ByMimeTypeCodec<>(map, "application/json");
		}

		@Override
		public byte[] parse(RestRequest request) {
			return byMimeType.parse(request);
		}

		@Override
		public void encode(byte[] object, RestRequest request) {
			byMimeType.encode(object, request);
		}

	}

	public static final class StreamFactory implements BodyParameterCodec.Factory<Stream> {

		@Override
		public BodyParameterCodec<?> create(Class<?> parameterType, Type type) {
			if (parameterType == Stream.class) {
				return StreamBodyCodec.INSTANCE;
			} else {
				return null;
			}
		}

	}

	public static final class ByteArrayFactory implements BodyParameterCodec.Factory<byte[]> {

		@Override
		public BodyParameterCodec<?> create(Class<?> parameterType, Type type) {
			if (parameterType == byte[].class) {
				return new ByteArrayBodyCodec();
			} else {
				return null;
			}
		}

	}

	public static final class StringFactory implements BodyParameterCodec.Factory<String> {

		@Override
		public BodyParameterCodec<?> create(Class<?> parameterType, Type type) {
			if (parameterType == String.class) {
				return new StringBodyCodec();
			} else {
				return null;
			}
		}

	}

	public static final class ObjectFactory implements BodyParameterCodec.Factory<Object> {

		@Override
		public BodyParameterCodec<?> create(Class<?> parameterType, Type type) {
			return new ObjectBodyCodec<>(type);
		}

	}
}
