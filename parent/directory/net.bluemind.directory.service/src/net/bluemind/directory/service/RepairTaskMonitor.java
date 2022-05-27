/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.directory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.RepairConfig;

public class RepairTaskMonitor implements IServerTaskMonitor {

	private static final Logger logger = LoggerFactory.getLogger(RepairTaskMonitor.class);
	private final IServerTaskMonitor delegate;
	private final String logPrefix;
	public final RepairConfig config;
	private boolean success;

	public RepairTaskMonitor(IServerTaskMonitor delegate, RepairConfig config) {
		this("", delegate, config);
	}

	public RepairTaskMonitor(String logPrefix, IServerTaskMonitor delegate, RepairConfig config) {
		this.logPrefix = logPrefix;
		this.delegate = delegate;
		this.config = config;
		this.success = true;
	}

	@Override
	public IServerTaskMonitor subWork(double work) {
		return new RepairTaskMonitor(delegate.subWork(work), config);
	}

	@Override
	public IServerTaskMonitor subWork(String logPrefix, double work) {
		return new RepairTaskMonitor(logPrefix, delegate.subWork(logPrefix, work), config);
	}

	@Override
	public void begin(double totalWork, String log) {
		if (config.logToCoreLog) {
			logger.info("[BEGIN][{}]: {} {}", totalWork, logPrefix, log);
		}
		if (config.verbose) {
			Level level = config.verbose ? Level.INFO : Level.DEBUG;
			delegate.begin(totalWork, log, level);
		}
	}

	@Override
	public void progress(double doneWork, String log) {
		if (config.logToCoreLog) {
			logger.info("[PROGRESS][{}]: {} {}", doneWork, logPrefix, log);
		}
		if (config.verbose) {
			Level level = config.verbose ? Level.INFO : Level.DEBUG;
			delegate.progress(doneWork, log, level);
		}
	}

	@Override
	public void end(boolean success, String log, String result) {
		boolean isSuccess = this.isSuccess() && success;
		if (config.logToCoreLog) {
			logger.info("[END][SUCCESS: {}][{}]: {}", isSuccess, result, logPrefix);
		}
		delegate.end(isSuccess, log, result);
	}

	public void end() {
		this.end(this.isSuccess(), null, null);
	}

	boolean isSuccess() {
		boolean isSuccess = this.success;
		if (delegate instanceof RepairTaskMonitor) {
			isSuccess &= ((RepairTaskMonitor) delegate).isSuccess();
		}
		return isSuccess;
	}

	@Override
	public void log(String log) {
		if (config.verbose) {
			if (config.logToCoreLog) {
				logger.info("{} {}", logPrefix, log);
			}
			delegate.log(log);
		}
	}

	@Override
	public void begin(double totalWork, String log, Level level) {
		if (config.logToCoreLog) {
			logger.info("[BEGIN][{}]: {} {}", totalWork, logPrefix, log);
		}
		if (config.verbose) {
			delegate.begin(totalWork, log, level);
		}
	}

	@Override
	public void progress(double doneWork, String log, Level level) {
		if (config.logToCoreLog) {
			logger.info("[PROGRESS][{}]: {} {}", doneWork, logPrefix, log);
		}
		if (config.verbose) {
			delegate.progress(doneWork, log, level);
		}
	}

	@Override
	public void end(boolean success, String log, String result, Level level) {
		if (config.logToCoreLog) {
			logger.info("[END][SUCCESS: {}][{}]: {} {}", success, result, logPrefix, log);
		}
		if (config.verbose) {
			delegate.end(success, log, result, level);
		}
	}

	@Override
	public void log(String log, Level level) {
		if (config.verbose) {
			if (config.logToCoreLog) {
				logger.info("[{}] {}", logPrefix, log);
			}
			delegate.log(log, level);
		}
	}

	@Override
	public void log(String log, Throwable t) {
		if (config.logToCoreLog) {
			logger.warn("{} {}", logPrefix, log, t);
		}
		delegate.log(log, t);
		notify(logPrefix + " log: " + t.getMessage());
	}

	@Override
	public void log(String format, Object... params) {
		if (config.verbose) {
			if (config.logToCoreLog) {
				Object[] parameters = new Object[params.length + 1];
				parameters[0] = logPrefix;
				System.arraycopy(params, 0, parameters, 1, params.length);
				logger.info("{} " + format, parameters);
			}
			delegate.log(format, params);
		}
	}

	public void notify(String log, Object... params) {
		this.success = false;
		if (config.logToCoreLog) {
			logger.warn(log, params);
		}
		delegate.log(MessageFormatter.arrayFormat(log, params).getMessage());
	}

}
