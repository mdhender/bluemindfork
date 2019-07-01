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

import org.vertx.java.core.json.JsonObject;

public class MonitorMessage {

	public static enum MessageType {
		begin, progress, end, log
	}

	public static JsonObject begin(double work, String message) {
		JsonObject m = new JsonObject();
		m.putString("type", MessageType.begin.name());
		m.putNumber("work", work);
		m.putString("message", message);
		return m;
	}

	public static JsonObject progress(double step, String message) {
		JsonObject m = new JsonObject();
		m.putString("type", MessageType.progress.name());
		m.putNumber("step", step);
		m.putString("message", message);
		return m;
	}

	public static JsonObject log(String message) {
		JsonObject m = new JsonObject();
		m.putString("type", MessageType.log.name());
		m.putString("message", message);
		return m;
	}

	public static JsonObject end(boolean success, String message) {
		return end(success, message, null);
	}

	public static JsonObject end(boolean success, String message, String result) {
		JsonObject m = new JsonObject();
		m.putString("type", MessageType.end.name());
		m.putBoolean("success", success);
		m.putString("message", message);

		if (result != null) {
			m.putString("result", result);
		}
		return m;
	}
}
