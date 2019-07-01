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

import java.math.BigDecimal;
import java.math.MathContext;

public class SubTaskMonitor extends AbstractTaskMonitor {

	private ISubTaskMonitorParent parentMonitor;
	private double parentWork;
	private double totalWork;
	private double parentTotalDone;
	private String logPrefix;

	public SubTaskMonitor(int depth, String logPrefix, double work, double subTotalWork,
			ISubTaskMonitorParent parentMonitor) {
		super(depth);
		this.parentWork = work;
		this.totalWork = subTotalWork;
		this.parentMonitor = parentMonitor;
		if (logPrefix != null && logPrefix.length() > 0) {
			this.logPrefix = logPrefix + " : ";
		} else {
			this.logPrefix = "";
		}
	}

	@Override
	public void begin(double totalWork, String log) {
		// maybe we could change local sub task totalWork
		this.totalWork = totalWork;
		if (log != null) {
			parentMonitor.log(logPrefix + log);
		}
	}

	@Override
	public void progress(double doneWork, String log) {

		BigDecimal work = BigDecimal.ZERO;
		if (totalWork != 0) {
			work = BigDecimal.valueOf(parentWork).multiply(BigDecimal.valueOf(doneWork))
					.divide(BigDecimal.valueOf(totalWork), MathContext.DECIMAL32);
			this.parentTotalDone = BigDecimal.valueOf(this.parentTotalDone).add(work).doubleValue();
			if (this.parentTotalDone > parentWork) {
				if (LoggingTaskMonitor.logger.isDebugEnabled()) {
					LoggingTaskMonitor.logger.debug("sub task overflow...", new Exception());
				}
				this.parentTotalDone = parentWork;
				work = BigDecimal.ZERO;
			}
		}
		LoggingTaskMonitor.logger.debug("parent progress done {} on {}", parentTotalDone, parentWork);
		if (log != null) {
			parentMonitor.progress(work.doubleValue(), logPrefix + log);
		} else {
			parentMonitor.progress(work.doubleValue(), null);
		}
	}

	@Override
	public void end(boolean success, String log, String result) {
		parentMonitor.childEnded();
	}

	public void flush() {
		if (parentWork - parentTotalDone > 0) {
			LoggingTaskMonitor.logger.debug("flush sub task {} on {}", parentWork - parentTotalDone, parentWork);
			parentMonitor.progress(parentWork - parentTotalDone, null);
		} else {
			LoggingTaskMonitor.logger.debug("no flush sub task {} on {}", parentWork - parentTotalDone, parentWork);
		}
	}

	@Override
	public void log(String log) {
		parentMonitor.log(log);
	}

}
