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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.base.codec.DefaultResponseCodecs;
import net.bluemind.core.rest.base.codec.ResponseCodec;
import net.bluemind.core.rest.model.RestServiceApiDescriptor.MethodDescriptor;

public abstract class ResponseBuilder {
	private static final Logger logger = LoggerFactory.getLogger(ResponseBuilder.class);

	public abstract RestResponse buildSuccess(RestRequest request, Object response) throws Exception;

	public abstract RestResponse buildFailure(RestRequest request, Throwable failure);

	public static JsonObject buildFault(Throwable e) {

		JsonObject object = new JsonObject();
		if (e instanceof ServerFault) {
			ServerFault fault = (ServerFault) e;
			if (((ServerFault) e).getCode() != null) {
				object.put("errorCode", fault.getCode().toString());

			} else {
				object.put("errorCode", ErrorCode.UNKNOWN.toString());
			}
		} else {
			object.put("errorCode", ErrorCode.UNKNOWN.toString());
		}
		object.put("errorType", e.getClass().getSimpleName());
		object.put("message", e.getMessage());
		return object;
	}

	public static RestResponse replyFault(int statusCode, String statusMessage, JsonObject body) {
		return RestResponse.fault(statusCode, statusMessage, Buffer.buffer(body.encode()));
	}

	public static RestResponse replyFault(int statusCode, String statusMessage, Throwable e) {
		return replyFault(statusCode, statusMessage, buildFault(e));
	}

	public static RestResponse replyServerFault(ServerFault e) {
		if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
			return replyFault(403, e.getMessage(), buildFault(e));
		} else {
			return replyFault(500, e.getMessage(), buildFault(e));
		}
	}

	private static class CodecResponseBuilder<T> extends ResponseBuilder {

		private ResponseCodec<T> codec;
		private String defaultMimeType;

		public CodecResponseBuilder(ResponseCodec<T> codec, String defaultMimeType) {
			this.codec = codec;
			this.defaultMimeType = defaultMimeType;
		}

		@Override
		public RestResponse buildSuccess(RestRequest request, Object response) throws Exception {
			if (response instanceof Throwable) {
				return codec.encodeFault(request, defaultMimeType, (Throwable) response);
			} else {
				return codec.encode(request, defaultMimeType, (T) response);
			}
		}

		@Override
		public RestResponse buildFailure(RestRequest request, Throwable cause) {
			return codec.encodeFault(request, defaultMimeType, cause);
		}
	}

	public static ResponseBuilder getResponseBuilder(MethodDescriptor methodDescriptor) {
		String[] producesType = methodDescriptor.produces;
		Type returnType = methodDescriptor.interfaceMethod.getGenericReturnType();

		if (methodDescriptor.genericType != null) {

			if (returnType.getTypeName().equals("net.bluemind.core.container.model.ItemValue<T>")) {
				returnType = methodDescriptor.genericType;
			} else if (returnType.getTypeName()
					.equals("java.util.List<net.bluemind.core.container.model.ItemValue<T>>")) {
				returnType = toListType(methodDescriptor.genericType);
			}
		}

		ResponseCodec<?> codec = DefaultResponseCodecs.codec(returnType, producesType[0]);
		return new CodecResponseBuilder<>(codec, methodDescriptor.produces[0]);

	}

	private static Type toListType(Type type) {
		return new ParameterizedType() {

			@Override
			public String getTypeName() {
				return "java.util.List<" + type.getTypeName() + ">";
			}

			@Override
			public Type[] getActualTypeArguments() {
				return new Type[] { type };

			}

			@Override
			public Type getRawType() {
				return List.class;
			}

			@Override
			public Type getOwnerType() {
				return null;
			}

		};

	}
}
