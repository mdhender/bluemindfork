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
package net.bluemind.node.client.tests;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class StatRecorder {

	private final AtomicLong ok = new AtomicLong(0);
	private final AtomicLong ko = new AtomicLong(0);
	private final Queue<Throwable> thrown = new ConcurrentLinkedQueue<>();

	public void ok() {
		ok.incrementAndGet();
	}

	public void ko(Throwable t) {
		ko.incrementAndGet();
		if (t != null) {
			thrown.add(t);
		}
	}

	public Collection<Throwable> getThrown() {
		return thrown;
	}

	public boolean hasFailed() {
		return ko.get() > 0;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[ok: ").append(ok.get()).append(", ko: ").append(ko.get()).append(", exceptions: ")
				.append(thrown.size()).append("].");
		if (!thrown.isEmpty()) {
			for (Throwable t : thrown) {
				sb.append('\n');
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);
				pw.flush();
				sb.append(sw.toString());
			}
		}

		return sb.toString();
	}

}
