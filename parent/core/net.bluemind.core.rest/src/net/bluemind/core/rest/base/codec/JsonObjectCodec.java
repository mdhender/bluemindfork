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

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.base.RestRequest;
import net.bluemind.core.rest.base.RestResponse;
import net.bluemind.core.utils.JsonUtils;

public class JsonObjectCodec {

	public static class Body<T> implements BodyParameterCodec<T> {

		private Type bodyType;

		Body(Type bodyType) {
			this.bodyType = bodyType;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T parse(RestRequest request) {
			if (request.body == null || request.body.length() == 0) {
				return null;
			}
			try {
				return (T) JsonUtils.read(request.body.toString("UTF-8"), bodyType);
			} catch (Exception e) {
				throw new IllegalArgumentException(String.format("Cannot convert %s of type %s to JSON: %s",
						request.body, bodyType.getClass().getName(), e.getMessage()));
			}
		}

		@Override
		public void encode(T object, RestRequest request) {
			if (object == null) {
				request.body = null;
			} else {
				request.body = Buffer.buffer(JsonUtils.asBuffer(object));
			}
		}

	}

	public static class Response<T> implements ResponseCodec<T> {

		private Type type;

		Response(Type type) {
			this.type = type;
		}

		@Override
		public RestResponse encode(RestRequest request, String defaultMimeType, T response) {
			if (response == null) {
				return RestResponse.ok(204, null);
			} else {
				return RestResponse.ok("application/json", 200, Buffer.buffer(JsonUtils.asBuffer(response)));
			}
		}

		@Override
		public T decode(RestResponse response) throws ServerFault {
			if (response.statusCode >= 400) {
				throw parseFault(response);
			}
			if (response.data == null || response.data.length() == 0) {
				return null;
			} else {
				try {
					return (T) JsonUtils.read(response.data.toString(), type);
				} catch (Exception e) {
					throw new CodecParseException(e);
				}
			}
		}

		@Override
		public RestResponse encodeFault(RestRequest request, String defaultMimeType, Throwable fault) {
			return replyFault(fault);
		}

	}

	public static ServerFault parseFault(RestResponse response) {

		try {
			JsonObject obj = new JsonObject(response.data.toString());
			ServerFault sf = new ServerFault(obj.getString("message"));
			String code = obj.getString("errorCode");
			if (code != null) {
				sf.setCode(ErrorCode.valueOf(code));
			}
			return sf;
		} catch (Exception e) {
			return new ServerFault(response.data.toString());
		}
	}

	public static final RestResponse replyFault(Throwable fault) {
		if (fault instanceof ServerFault) {
			ServerFault sf = (ServerFault) fault;
			if (sf.getCode() == ErrorCode.PERMISSION_DENIED) {
				return replyFault(403, fault.getMessage(), buildFault(fault));
			} else if (sf.getCode() == ErrorCode.NOT_FOUND) {
				return replyFault(404, fault.getMessage(), buildFault(fault));
			} else {
				return replyFault(500, fault.getMessage(), buildFault(fault));
			}
		} else {
			return replyFault(500, fault.getMessage(), buildFault(fault));
		}
	}

	public static JsonObject buildFault(Throwable e) {

		JsonObject object = new JsonObject();
		if (e instanceof ServerFault) {
			ServerFault fault = (ServerFault) e;
			if (((ServerFault) e).getCode() != null) {
				object.put("errorCode", fault.getCode().toString());

			} else {
				object.put("errorCode", ErrorCode.UNKNOWN.toString());
			}
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
		return replyFault(500, e.getMessage(), buildFault(e));
	}
}
