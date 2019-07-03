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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.streams.ReadStream;

import net.bluemind.lib.vertx.BMExecutor.BMTaskMonitor;

public class MonitoredReadStream implements ReadStream<MonitoredReadStream> {

	private static final Logger logger = LoggerFactory.getLogger(MonitoredReadStream.class);
	private ReadStream<?> monitored;
	private BMTaskMonitor monitor;

	public MonitoredReadStream(ReadStream<?> stream, BMTaskMonitor monitor) {
		this.monitored = stream;
		this.monitor = monitor;
	}

	@Override
	public MonitoredReadStream dataHandler(Handler<Buffer> handler) {
		monitored.dataHandler(buff -> {
			if (!monitor.alive()) {
				logger.warn("stream out during data transfer");
				throw new RuntimeException("call timouted");
			}
			handler.handle(buff);
		});
		return this;
	}

	@Override
	public MonitoredReadStream pause() {
		monitored.pause();
		return this;
	}

	@Override
	public MonitoredReadStream resume() {
		monitored.resume();
		return this;
	}

	@Override
	public MonitoredReadStream exceptionHandler(Handler<Throwable> handler) {
		monitored.exceptionHandler(handler);
		return this;
	}

	@Override
	public MonitoredReadStream endHandler(Handler<Void> endHandler) {
		monitored.endHandler(endHandler);
		return this;
	}

}
