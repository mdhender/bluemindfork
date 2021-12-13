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
package net.bluemind.core.task.service.internal;

import io.vertx.core.json.JsonObject;

public class MonitorMessage {

	public enum MessageType {
		begin, progress, end, log
	}

	public static JsonObject begin(String taskId, double work, String message) {
		JsonObject m = new JsonObject();
		m.put("id", taskId);
		m.put("type", MessageType.begin.name());
		m.put("work", work);
		m.put("message", message);
		return m;
	}

	public static JsonObject progress(String taskId, double step, String message) {
		JsonObject m = new JsonObject();
		m.put("id", taskId);
		m.put("type", MessageType.progress.name());
		m.put("step", step);
		m.put("message", message);
		return m;
	}

	public static JsonObject log(String taskId, String message) {
		JsonObject m = new JsonObject();
		m.put("id", taskId);
		m.put("type", MessageType.log.name());
		m.put("message", message);
		return m;
	}

	public static JsonObject end(String taskId, boolean success, String message) {
		return end(taskId, success, message, null);
	}

	public static JsonObject end(String taskId, boolean success, String message, String result) {
		JsonObject m = new JsonObject();
		m.put("id", taskId);
		m.put("type", MessageType.end.name());
		m.put("success", success);
		m.put("message", message);

		if (result != null) {
			m.put("result", result);
		}
		return m;
	}
}
