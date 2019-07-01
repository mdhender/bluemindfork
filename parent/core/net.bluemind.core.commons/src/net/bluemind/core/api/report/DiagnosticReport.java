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
package net.bluemind.core.api.report;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3.0")
public class DiagnosticReport {

	@BMApi(version = "3.0")
	public static enum State {
		OK, KO, WARN
	}

	@BMApi(version = "3.0")
	public static class Entry {

		@Override
		public String toString() {
			return "Entry [id=" + id + ", state=" + state + ", message=" + message + "]";
		}

		public String id;
		public State state;
		public String message;
		public long timestamp;
		public String[] stack;

		public static Entry create(String id, String msg, State state, Exception exception) {
			Entry ret = new Entry();
			ret.id = id;
			ret.state = state;
			ret.message = msg;
			ret.timestamp = System.currentTimeMillis();
			ret.stack = exception != null
					? Arrays.stream(exception.getStackTrace()).map(ste -> ste.toString()).toArray(String[]::new) : null;
			return ret;
		}

		public static Entry ok(String id, String msg) {
			return create(id, msg, State.OK, null);
		}

		public static Entry ko(String id, String msg) {
			return create(id, msg, State.KO, null);
		}

		public static Entry ko(String id, String msg, Exception stack) {
			return create(id, msg, State.KO, stack);
		}

		public static Entry warn(String id, String msg) {
			return create(id, msg, State.WARN, null);
		}

	}

	public List<Entry> entries = new LinkedList<>();

	public static DiagnosticReport create() {
		DiagnosticReport r = new DiagnosticReport();
		r.entries = new LinkedList<>();
		return r;
	}

	public void ko(String id, String msg) {
		entries.add(Entry.ko(id, msg));
	}

	public void ok(String id, String msg) {
		entries.add(Entry.ok(id, msg));
	}

	public void warn(String id, String msg) {
		entries.add(Entry.warn(id, msg));
	}

	public State globalState() {
		State ret = State.OK;

		for (Entry entry : entries) {
			if (entry.state != State.OK) {
				ret = entry.state;
				break;
			}
		}

		return ret;
	}

}
