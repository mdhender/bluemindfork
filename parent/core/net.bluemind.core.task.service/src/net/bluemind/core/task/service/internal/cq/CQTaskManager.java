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
package net.bluemind.core.task.service.internal.cq;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.base.MoreObjects;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.task.service.internal.LogStream;
import net.bluemind.core.task.service.internal.TaskManager;

public class CQTaskManager extends TaskManager implements Handler<Message<JsonObject>> {

	private final PersistentQueue jsQueue;

	private ConcurrentLinkedQueue<LogStream> readers = new ConcurrentLinkedQueue<>();

	public CQTaskManager(String taskId) {
		super(taskId);
		this.jsQueue = PersistentQueue.createFor(taskId);
	}

	@Override
	public void cleanUp() {
		jsQueue.close();
	}

	public ReadStream<Buffer> log() {
		LogStream reader = new LogStream(jsQueue.subscriber(0));
		registerReader(reader);
		reader.exceptionHandler(ex -> {
			this.readers.remove(reader);
		});
		return reader;
	}

	public List<String> getCurrentLogs(int offset) {
		List<String> ret = new ArrayList<>(64);
		// we add blanks so offsets are consistent
		jsQueue.subscriber(offset).fetchAll(js -> ret.add(Optional.ofNullable(js.getString("message")).orElse("")));
		return ret;
	}

	private void registerReader(LogStream reader) {
		this.readers.add(reader);
	}

	protected void pushLog(JsonObject log, boolean end) {
		jsQueue.put(log);
		for (LogStream reader : readers) {
			reader.wakeUp();
			if (end) {
				reader.end();
			}
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(CQTaskManager.class).add("taskId", taskId).add("status", status)
				.add("queue", jsQueue).toString();
	}
}