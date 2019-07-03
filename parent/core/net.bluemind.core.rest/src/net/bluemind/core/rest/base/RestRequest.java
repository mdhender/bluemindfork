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

import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

public class RestRequest {
	public Buffer body;
	public ReadStream<?> bodyStream;
	public MultiMap params;
	public String path;
	public MultiMap headers;
	public String method;
	public List<String> remoteAddresses;
	public String origin;

	public RestRequest(String origin, List<String> remoteAddresses, String method, MultiMap headers, String path,
			MultiMap params, Buffer body, ReadStream<?> bodyStream) {
		this.origin = origin;
		this.remoteAddresses = remoteAddresses;
		this.method = method;
		this.headers = headers;
		this.path = path;
		this.params = params;
		this.body = body;
		this.bodyStream = bodyStream;
	}

	@Override
	public String toString() {
		return String.format("RestRequest [path=%s, method=%s, headers=%s, params=%s, remoteAddresses=%s, origin=%s]",
				path, method, headers.toString(), params, remoteAddresses, origin);
	}

	public static RestRequest create(String remoteAddress, String method, MultiMap headers, String path,
			MultiMap params, Buffer body, ReadStream<?> bodyStream) {
		// would it break if we replace this copy by wrapping into an immutable ?
		MultiMap fastMultimapForHeaders = RestHeaders.newMultimap();
		fastMultimapForHeaders.add(headers);

		List<String> forwardedFor = new ArrayList<>(headers.getAll(RestHeaders.X_FORWARDED_FOR));
		forwardedFor.add(remoteAddress);

		String origin = headers.get(RestHeaders.X_BM_ORIGIN);
		return new RestRequest(origin, forwardedFor, method, fastMultimapForHeaders, path, params, body, bodyStream);
	}

}
