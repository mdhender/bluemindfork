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

import java.util.UUID;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.core.rest.base.RestResponse;

@SuppressWarnings("serial")
public class VertxRestResponse extends JsonObject {

	public final Buffer responseData;
	public final String responseStreamAdr;
	public final MultiMap headers;
	public final int statusCode;

	public VertxRestResponse(MultiMap headers, int statusCode, Buffer responseData, String responseStreamAdr) {
		this.headers = headers;
		this.statusCode = statusCode;
		this.responseData = responseData;
		this.responseStreamAdr = responseStreamAdr;
	}

	@Override
	public String toString() {
		return String.format("VertxRestResponse [code=%d]", statusCode);
	}

	@Override
	public String encode() {
		throw new RuntimeException("should not be called");
	}

	@Override
	public JsonObject copy() {
		return this;
	}

	public static VertxRestResponse create(RestResponse response) {
		if (response.responseStream == null) {
			return new VertxRestResponse(response.headers, response.statusCode, response.data, null);
		} else {
			String adr = UUID.randomUUID().toString();
			return new VertxRestResponse(response.headers, response.statusCode, response.data, adr);

		}
	}

	public RestResponse asResponse() {
		RestResponse response = RestResponse.ok(statusCode, responseData);
		response.headers = headers;
		return response;
	}

}
