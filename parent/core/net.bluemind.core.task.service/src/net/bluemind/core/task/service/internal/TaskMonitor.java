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

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;

import net.bluemind.core.task.service.AbstractTaskMonitor;
import net.bluemind.core.task.service.LoggingTaskMonitor;

public class TaskMonitor extends AbstractTaskMonitor {

	private EventBus eventBus;
	private String address;
	private Handler<Void> endHandler;
	private boolean ended;

	public TaskMonitor(EventBus eventBus, String address) {
		super(0);
		this.eventBus = eventBus;
		this.address = address;
	}

	@Override
	public void begin(double work, String log) {
		LoggingTaskMonitor.logger.debug("send begin {} {} {}", address, work, log);
		eventBus.publish(address, MonitorMessage.begin(work, log));
	}

	@Override
	public void progress(double step, String log) {
		LoggingTaskMonitor.logger.debug("send progress {} {} {}", address, step, log);
		eventBus.publish(address, MonitorMessage.progress(step, log));
	}

	@Override
	public void log(String log) {
		LoggingTaskMonitor.logger.debug("send log {} {}", address, log);
		eventBus.publish(address, MonitorMessage.log(log));

	}

	@Override
	public void end(boolean success, String log, String result) {
		if (ended) {
			return;
		}
		ended = true;
		LoggingTaskMonitor.logger.debug("send end {} {} result {}", address, log, result);
		eventBus.publish(address, MonitorMessage.end(success, log, result));
		if (endHandler != null)
			endHandler.handle(null);

	}

	public void endHandler(Handler<Void> endHandler) {
		this.endHandler = endHandler;
	}

	public boolean ended() {
		return ended;
	}

}
