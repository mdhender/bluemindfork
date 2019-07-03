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
package net.bluemind.node.client.impl.ahc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import net.bluemind.common.io.FileBackedOutputStream;
import com.ning.http.client.HttpResponseHeaders;

import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;

public class StatusHandler extends DefaultAsyncHandler<TaskStatus> {

	private static final Logger logger = LoggerFactory.getLogger(StatusHandler.class);
	private final String pid;

	private static final JsonArray EMPTY_ARRAY = new JsonArray();

	public StatusHandler(String pid) {
		super(true);
		this.pid = pid;
	}

	@Override
	protected TaskStatus getResult(int status, HttpResponseHeaders headers, FileBackedOutputStream body) {
		try {
			byte[] bytes = body.asByteSource().read();
			JsonObject jso = new JsonObject(new String(bytes));
			boolean complete = jso.getBoolean("complete");
			boolean successfull = jso.getBoolean("successful");
			int exitCode = jso.getInteger("exitCode", 1);
			JsonArray output = jso.getArray("output", EMPTY_ARRAY);
			String lastLogEntry = Joiner.on('\n').join(output);
			State state = fromBooleans(complete, successfull);
			TaskStatus ts = TaskStatus.create(10, state.ended ? 10 : 1, lastLogEntry, state, "" + exitCode);
			return ts;
		} catch (Exception e) {
			logger.error("[{}] Status check failure.", pid);
			throw Throwables.propagate(e);
		} finally {
			try {
				body.reset();
			} catch (IOException e) {
			}
		}
	}

	private TaskStatus.State fromBooleans(boolean complete, boolean successfull) {
		for (State st : TaskStatus.State.values()) {
			if (st.ended == complete && st.succeed == successfull) {
				return st;
			}
		}
		return State.InError;
	}
}
