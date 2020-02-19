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
package net.bluemind.core.task.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTaskMonitor extends AbstractTaskMonitor {
	private static final String[] depthPattern;
	public static final Logger logger = LoggerFactory.getLogger(LoggingTaskMonitor.class);

	static {
		depthPattern = new String[10];
		StringBuilder t = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			depthPattern[i] = t.toString();
			t.append("\t");
		}
	}

	private final IServerTaskMonitor delegate;
	private final Logger instanceLogger;
	private int childDepth;
	private double totalWork;
	private double done;

	public LoggingTaskMonitor(Logger logger, IServerTaskMonitor delegate, int childDepth) {
		super(childDepth);
		if (logger == null) {
			this.instanceLogger = LoggingTaskMonitor.logger;
		} else {
			this.instanceLogger = logger;
		}
		this.childDepth = childDepth;
		this.delegate = delegate;
	}

	@Override
	public void begin(double totalWork, String log) {
		this.totalWork = totalWork;
		instanceLogger.debug("{}BEGIN: total work: {}, {}", depthPattern[childDepth], totalWork, log);
		delegate.begin(totalWork, log);
	}

	@Override
	public void progress(double doneWork, String log) {
		done += doneWork;
		instanceLogger.debug("{}PROGRESS: done work {}, progress {} on {}, {}", depthPattern[childDepth], doneWork,
				done, totalWork, log);
		delegate.progress(doneWork, log);
	}

	@Override
	public void end(boolean success, String log, String result) {
		instanceLogger.debug("{}END: {}, {}, Result: {}", depthPattern[childDepth], success, log, result);
		delegate.end(success, log, result);
	}

	@Override
	public void log(String log) {
		instanceLogger.debug("{}LOG: {}", depthPattern[childDepth], log);
		delegate.log(log);
	}
}
