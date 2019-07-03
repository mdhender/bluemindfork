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
package net.bluemind.eas.impl.vertx.compat;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;

import net.bluemind.eas.wbxml.WbxmlOutput;
import net.bluemind.vertx.common.request.Requests;

public class VertxOutput extends WbxmlOutput {

	private static final Logger logger = LoggerFactory.getLogger(VertxOutput.class);
	private static final long THRESHOLD = 32768;
	private final HttpServerResponse resp;
	private final HttpServerRequest req;
	private long count;
	private Buffer pending;
	private long total;

	public VertxOutput(HttpServerRequest req) {
		this.req = req;
		this.resp = req.response();
		this.pending = new Buffer();
	}

	@Override
	public void write(int b) throws IOException {
		pending.appendByte((byte) b);
		count += 1;
		total += 1;
		flushIfNecessary(null);
	}

	private void flushIfNecessary(final QueueDrained drained) {
		if (count > THRESHOLD) {
			resp.write(pending);
			pending = new Buffer();
			count = 0;
			if (drained != null) {
				if (resp.writeQueueFull()) {
					logger.warn("GOT QUEUE FULL condition");
					resp.drainHandler(new Handler<Void>() {

						@Override
						public void handle(Void event) {
							drained.drained();
						}
					});
				} else {
					drained.drained();
				}
			}
		} else if (drained != null) {
			drained.drained();
		}
	}

	@Override
	public void write(byte[] data) throws IOException {
		int len = data.length;
		count += len;
		total += len;
		pending.appendBytes(data, 0, len);
		flushIfNecessary(null);
	}

	@Override
	public void write(byte[] data, QueueDrained drained) {
		int len = data.length;
		count += len;
		total += len;
		pending.appendBytes(data, 0, len);
		flushIfNecessary(drained);
	}

	@Override
	public String end() {
		Requests.tag(req, "out.size", String.format("%db", total));
		String requestIdentifier = Requests.tag(req, "rid");
		resp.end(pending);
		return requestIdentifier;
	}

}
