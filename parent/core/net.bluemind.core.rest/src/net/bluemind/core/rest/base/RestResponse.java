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

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;

public class RestResponse {

	public Buffer data;
	public ReadStream<Buffer> responseStream = null;

	public MultiMap headers = new CaseInsensitiveHeaders();
	public final int statusCode;

	public RestResponse(int statusCode) {
		this.statusCode = statusCode;
	}

	public static RestResponse fault(Throwable e) {
		return ResponseBuilder.replyFault(500, "server error", e);
	}

	public static RestResponse invalidSession(String key) {
		return ResponseBuilder.replyFault(401, key, new ServerFault(key, ErrorCode.AUTHENTICATION_FAIL));
	}

	public static RestResponse incompatibleClient(String message) {
		return ResponseBuilder.replyFault(409, message, new ServerFault(message, ErrorCode.FAILURE));
	}

	public static RestResponse fault(int statusCode, String statusMessage, Buffer buffer) {
		RestResponse ret = new RestResponse(statusCode);
		ret.data = buffer;
		return ret;
	}

	public static RestResponse ok(int statusCode, Buffer buffer) {
		RestResponse ret = new RestResponse(statusCode);
		ret.data = buffer;
		return ret;
	}

	public static RestResponse ok(String mimeType, int statusCode, Buffer buffer) {
		RestResponse ret = new RestResponse(statusCode);
		ret.headers.add("Content-Type", mimeType);
		ret.data = buffer;
		return ret;
	}

	public static RestResponse ok(MultiMap headers, int statusCode, Buffer buffer) {
		RestResponse ret = new RestResponse(statusCode);
		ret.headers = new CaseInsensitiveHeaders();
		ret.headers.addAll(headers);
		ret.data = buffer;
		return ret;
	}

	public static RestResponse stream(ReadStream<Buffer> stream) {
		RestResponse ret = new RestResponse(200);
		ret.responseStream = stream;
		return ret;
	}

	@Override
	public String toString() {
		return String.format("RestResponse [headers=%s]", headers.toString());
	}
}
