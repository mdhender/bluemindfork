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
package net.bluemind.core.rest.vertx;

import java.util.List;
import java.util.UUID;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.rest.base.RestRequest;

@SuppressWarnings("serial")
public class VertxRestRequest extends JsonObject {
	public final Buffer body;
	public final MultiMap params;
	public final String path;
	public final MultiMap headers;
	public final HttpMethod method;
	public final List<String> remoteAddresses;
	public final String bodyStreamAdr;
	public String origin;

	public VertxRestRequest(String origin, List<String> remoteAddresses, HttpMethod method, MultiMap headers,
			String path, MultiMap params, Buffer body, String bodyStreamAdr) {
		this.origin = origin;
		this.remoteAddresses = remoteAddresses;
		this.method = method;
		this.headers = headers;
		this.path = path;
		this.params = params;
		this.body = body;
		this.bodyStreamAdr = bodyStreamAdr;
	}

	@Override
	public String toString() {
		return String.format("RestRequest [path=%s, method=%]", path, method);
	}

	@Override
	public String encode() {
		throw new RuntimeException("should not be called");
	}

	@Override
	public JsonObject copy() {
		return this;
	}

	public static VertxRestRequest create(RestRequest request) {
		if (request.bodyStream == null) {
			return new VertxRestRequest(request.origin, request.remoteAddresses, request.method, request.headers,
					request.path, request.params, request.body, null);
		} else {
			String adr = UUID.randomUUID().toString();
			return new VertxRestRequest(request.origin, request.remoteAddresses, request.method, request.headers,
					request.path, request.params, request.body, adr);

		}
	}

	public RestRequest asRestRequest(ReadStream<Buffer> bodyStream) {
		return new RestRequest(origin, remoteAddresses, method, headers, path, params, body, bodyStream);
	}

	public static RestRequest decode(JsonObject request) {
		return null;
	}

}
