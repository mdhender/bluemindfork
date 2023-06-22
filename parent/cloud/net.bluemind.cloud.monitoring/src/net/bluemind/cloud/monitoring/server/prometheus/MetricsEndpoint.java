/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.cloud.monitoring.server.prometheus;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;

public class MetricsEndpoint implements Handler<HttpServerRequest> {

	private final CollectorRegistry registry;

	public MetricsEndpoint() {
		this.registry = CollectorRegistry.defaultRegistry;
	}

	@Override
	public void handle(HttpServerRequest request) {
		request.response().headers().add("Access-Control-Allow-Origin", "*");
		try {
			BufferWriter writer = new BufferWriter();
			String contentType = TextFormat.chooseContentType(request.headers().get("Accept"));

			TextFormat.writeFormat(contentType, writer, registry.filteredMetricFamilySamples(parse(request)));
			request.response().setStatusCode(200).putHeader("Content-Type", contentType).end(writer.getBuffer());
		} catch (IOException e) {
			request.response().setStatusCode(500);
			request.response().end(e.getMessage());
		}
	}

	private Set<String> parse(HttpServerRequest request) {
		return new HashSet<>(request.params().getAll("name[]"));
	}

	static class BufferWriter extends Writer {

		private final Buffer buffer = Buffer.buffer();

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			buffer.appendString(new String(cbuf, off, len));
		}

		@Override
		public void flush() throws IOException {
		}

		@Override
		public void close() throws IOException {
		}

		Buffer getBuffer() {
			return buffer;
		}
	}

}
