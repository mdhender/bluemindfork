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

import net.bluemind.core.task.service.IServerTaskMonitor;

public abstract class AbstractTaskMonitor implements ISubTaskMonitorParent {
	private static final Logger logger = LoggerFactory.getLogger(AbstractTaskMonitor.class);
	private SubTaskMonitor subMonitor;
	private int depth;

	public AbstractTaskMonitor(int depth) {
		this.depth = depth;
	}

	@Override
	final public IServerTaskMonitor subWork(double work) {
		return subWork(null, work);
	}

	@Override
	final public IServerTaskMonitor subWork(String logPrefix, double work) {
		if (subMonitor != null) {
			childEnded();
			if (logger.isTraceEnabled()) {
				logger.warn("subwork not ended !", new Exception());
			}
		}

		subMonitor = new SubTaskMonitor(depth + 1, logPrefix, work, 1, this);
		return subMonitor;
	}

	@Override
	final public void childEnded() {
		if (subMonitor != null) {
			subMonitor.flush();
			subMonitor = null;
		}
	}

}
