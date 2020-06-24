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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.net.HttpHeaders;

import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.rest.base.codec.ResponseCodec.Factory;
import net.bluemind.core.rest.vertx.VertxStream;

public class DefaultResponseCodecs {

	private static List<ResponseCodec.Factory<?>> factories = new ArrayList<>();

	static {
		factories.add(new VoidFactory());
		factories.add(new ByClassFactory<Stream>(Stream.class, new StreamResponseCodec<>()));
		factories.add(new ByClassFactory<String>(String.class, new ByMimeTypeResponseCodec<String>(
				new StringResponseCodec(), "application/json", new JsonObjectCodec.Response<String>(String.class))));
		factories.add(new ByClassFactory<byte[]>(byte[].class, new ByMimeTypeResponseCodec<byte[]>(
				new ByteArrayResponseCodec(), "application/json", new JsonObjectCodec.Response<byte[]>(byte[].class))));
		factories.add(new ObjectFactory<>());
	}

	public static ResponseCodec<?> codec(Type type, String defaultMimeType) {
		ResponseCodec<?> codec = null;
		for (Factory<?> factory : factories) {
			codec = factory.create(type, defaultMimeType);
			if (codec != null) {
				break;
			}
		}
		return codec;
	}

	public static class ObjectFactory<T> implements ResponseCodec.Factory<T> {

		@Override
		public ResponseCodec<?> create(Type type, String defaultMimeType) {
			return new JsonObjectCodec.Response<T>(type);
		}

	}

	public static class VoidFactory implements ResponseCodec.Factory<Void> {

		@Override
		public ResponseCodec<?> create(Type type, String defaultMimeType) {
			if (type == void.class) {
				return new VoidCodec();
			} else {
				return null;
			}
		}

	}

	public static class ByClassFactory<T> implements ResponseCodec.Factory<T> {

		private ResponseCodec<T> codec;
		private Class<T> klass;

		public ByClassFactory(Class<T> klass, ResponseCodec<T> codec) {
			this.klass = klass;
			this.codec = codec;
		}

		@Override
		public ResponseCodec<?> create(Type type, String defaultMimeType) {
			if (type == klass) {
				return codec;
			} else {
				return null;
			}
		}

	}

	public static class ByMimeTypeResponseCodec<T> implements ResponseCodec<T> {
		private ResponseCodec<T> defaultCodec;
		private Map<String, ResponseCodec<T>> codecsByMimetypes;

		public ByMimeTypeResponseCodec(ResponseCodec<T> defaultCodec, String mimeType, ResponseCodec<T> mimetypedCoec) {
			this.defaultCodec = defaultCodec;
			codecsByMimetypes = ImmutableMap.<String, ResponseCodec<T>>builder().put(mimeType, mimetypedCoec).build();
		}

		@Override
		public RestResponse encode(RestRequest request, String defaultMimeType, T response) {
			Map.Entry<String, ResponseCodec<T>> codec = codec(request, defaultMimeType);
			RestResponse resp = codec.getValue().encode(request, defaultMimeType, response);
			if (resp.headers.get("Content-Type") == null) {
				resp.headers.add("Content-Type", codec.getKey());
			}
			return resp;
		}

		@Override
		public RestResponse encodeFault(RestRequest request, String defaultMimeType, Throwable fault) {
			Map.Entry<String, ResponseCodec<T>> codec = codec(request, defaultMimeType);
			RestResponse resp = codec.getValue().encodeFault(request, defaultMimeType, fault);
			if (resp.headers.get("Content-Type") == null) {
				resp.headers.add("Content-Type", codec.getKey());
			}
			return resp;
		}

		private static final Splitter ACCEPT_SPLITTER = Splitter.on(", ");

		private Map.Entry<String, ResponseCodec<T>> codec(RestRequest request, String defaultMimeType) {
			String accept = request.headers.get(HttpHeaders.ACCEPT);
			if (accept != null) {
				Iterable<String> accepts = ACCEPT_SPLITTER.split(accept);
				if (!accept.isEmpty()) {
					return getBestCodec(accepts, defaultMimeType);
				}

			}
			return Maps.immutableEntry(defaultMimeType, defaultCodec);

		}

		private Map.Entry<String, ResponseCodec<T>> getBestCodec(Iterable<String> accepts, String defaultMimeType) {
			Map.Entry<String, ResponseCodec<T>> ret = null;

			for (String type : accepts) {
				ResponseCodec<T> possible = codecsByMimetypes.get(type);
				if (possible != null) {
					ret = Maps.immutableEntry(type, possible);
					break;
				}
			}

			if (ret == null) {

				ResponseCodec<T> possible = codecsByMimetypes.get(defaultMimeType);
				if (possible != null) {
					ret = Maps.immutableEntry(defaultMimeType, possible);
				}
			}

			if (ret == null) {
				ret = Maps.immutableEntry(defaultMimeType, defaultCodec);
			}
			return ret;
		}

		@Override
		public T decode(RestResponse response) throws ServerFault {
			String m = response.headers.get("Content-Type");
			ResponseCodec<T> codec = null;
			if (m != null) {
				codec = codecsByMimetypes.get(m);
			}

			if (codec == null) {
				codec = defaultCodec;
			}

			return codec.decode(response);
		}
	}

	public static class StringResponseCodec implements ResponseCodec<String> {

		@Override
		public RestResponse encode(RestRequest request, String defaultMimeType, String response) {
			if (response == null) {
				return RestResponse.ok(204, null);
			} else {
				return RestResponse.ok(defaultMimeType, 200, Buffer.buffer(response));
			}
		}

		@Override
		public String decode(RestResponse response) throws ServerFault {
			if (response.statusCode >= 400) {
				throw JsonObjectCodec.parseFault(response);
			}
			if (response.data == null) {
				return null;
			} else {
				return response.data.toString();
			}
		}

		@Override
		public RestResponse encodeFault(RestRequest request, String defaultMimeType, Throwable fault) {
			return JsonObjectCodec.replyFault(fault);
		}

	}

	public static class ByteArrayResponseCodec implements ResponseCodec<byte[]> {

		@Override
		public RestResponse encode(RestRequest request, String defaultMimeType, byte[] response) {
			if (response == null) {
				return RestResponse.ok(204, null);
			} else {
				return RestResponse.ok(defaultMimeType, 200, Buffer.buffer(Unpooled.wrappedBuffer(response)));
			}
		}

		@Override
		public byte[] decode(RestResponse response) throws ServerFault {
			if (response.statusCode >= 400) {
				throw JsonObjectCodec.parseFault(response);
			}

			if (response.data == null) {
				return null;
			} else {
				return response.data.getBytes();
			}
		}

		@Override
		public RestResponse encodeFault(RestRequest request, String defaultMimeType, Throwable fault) {
			return JsonObjectCodec.replyFault(fault);
		}

	}

	public static class StreamResponseCodec<T> implements ResponseCodec<Stream> {

		@Override
		public RestResponse encode(RestRequest request, String defaultMimeType, Stream response) {
			RestResponse resp = RestResponse.stream(VertxStream.read(response));
			String mime = response.mime().orElse(defaultMimeType);
			resp.headers.add("Content-Type", response.charset().map(cs -> mime + "; charset=" + cs).orElse(mime));
			response.fileName().ifPresent(fn -> {
				String sanitized = CharMatcher.ascii().retainFrom(fn);
				resp.headers.add("Content-Disposition", String.format("attachment; filename=\"%s\";", sanitized));
			});
			return resp;
		}

		@Override
		public Stream decode(RestResponse response) throws ServerFault {
			if (response.statusCode >= 400) {
				throw JsonObjectCodec.parseFault(response);
			}

			return VertxStream.stream(response.responseStream);
		}

		@Override
		public RestResponse encodeFault(RestRequest request, String defaultMimeType, Throwable fault) {
			return JsonObjectCodec.replyFault(fault);
		}

	}

	public static class VoidCodec implements ResponseCodec<Void> {

		@Override
		public RestResponse encode(RestRequest request, String defaultMimeType, Void response) {
			RestResponse resp = RestResponse.ok(200, Buffer.buffer());
			resp.headers.add("Content-Type", defaultMimeType);
			return resp;
		}

		@Override
		public Void decode(RestResponse response) throws ServerFault {
			if (response.statusCode >= 400) {
				throw JsonObjectCodec.parseFault(response);
			}

			return null;
		}

		@Override
		public RestResponse encodeFault(RestRequest request, String defaultMimeType, Throwable fault) {
			return JsonObjectCodec.replyFault(fault);
		}

	}
}
