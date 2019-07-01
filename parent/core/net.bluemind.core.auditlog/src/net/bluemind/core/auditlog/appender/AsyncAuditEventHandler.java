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
package net.bluemind.core.auditlog.appender;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import net.bluemind.core.auditlog.AuditEvent;

public class AsyncAuditEventHandler implements IAuditEventAppender {

	private final BlockingQueue<AuditEvent> eventsQueue = new ArrayBlockingQueue<>(100);

	private IAuditEventAppender appender;

	private Worker worker;

	private volatile boolean running;

	@Override
	public void write(AuditEvent event) {
		if (!running) {
			throw new IllegalStateException("appender is not running");
		}
		eventsQueue.add(event);
	}

	public void start() {
		if (running) {
			throw new IllegalStateException("appender is already running");
		}
		this.worker = new Worker();
		running = true;
		worker.start();
	}

	public void stop() {
		if (!running) {
			return;
		}
		running = false;
		try {
			worker.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	class Worker extends Thread {
		public Worker() {
			super("Async AuditEvent appender [" + appender.getClass() + "]");
		}

		@Override
		public void run() {
			while (running) {
				try {
					appender.write(eventsQueue.take());
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
			running = false;
			// flush
			for (AuditEvent v : eventsQueue) {
				appender.write(v);
			}

			eventsQueue.clear();
		}

	}
}
