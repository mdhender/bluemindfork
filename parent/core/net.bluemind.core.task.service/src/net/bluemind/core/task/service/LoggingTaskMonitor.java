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
import org.slf4j.event.Level;

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
		begin(totalWork, log, Level.DEBUG);
	}

	@Override
	public void begin(double totalWork, String log, Level level) {
		this.totalWork = totalWork;
		logWithLevel(level, "{}BEGIN: total work: {}, {}", depthPattern[childDepth], totalWork, log);
		delegate.begin(totalWork, log);
	}

	@Override
	public void progress(double doneWork, String log) {
		progress(doneWork, log, Level.DEBUG);
	}

	@Override
	public void progress(double doneWork, String log, Level level) {
		done += doneWork;
		logWithLevel(level, "{}PROGRESS: done work {}, progress {} on {}, {}", depthPattern[childDepth], doneWork, done,
				totalWork, log);
		delegate.progress(doneWork, log);
	}

	@Override
	public void end(boolean success, String log, String result) {
		end(success, log, result, Level.DEBUG);
	}

	@Override
	public void end(boolean success, String log, String result, Level level) {
		logWithLevel(level, "{}END: {}, {}, Result: {}", depthPattern[childDepth], success, log, result);
		delegate.end(success, log, result);
	}

	@Override
	public void log(String log) {
		log(log, Level.DEBUG);
	}

	@Override
	public void log(String log, Level level) {
		logWithLevel(level, "{}LOG: {}", depthPattern[childDepth], log);
		delegate.log(log);
	}

	@Override
	public void log(String log, Throwable t) {
		logWithLevel(Level.ERROR, "{}LOG: {}", depthPattern[childDepth], log, t);
		delegate.log(log + ":\n" + t.getMessage());
	}

	public void logWithLevel(Level level, String pattern, Object... args) {
		switch (level) {
		case ERROR:
			instanceLogger.error(pattern, args);
			break;
		case WARN:
			instanceLogger.warn(pattern, args);
			break;
		case INFO:
			instanceLogger.info(pattern, args);
			break;
		case DEBUG:
			instanceLogger.debug(pattern, args);
			break;
		case TRACE:
			instanceLogger.trace(pattern, args);
			break;
		}
	}
}
