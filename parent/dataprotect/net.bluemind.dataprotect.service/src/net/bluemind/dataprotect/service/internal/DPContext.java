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

package net.bluemind.dataprotect.service.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.service.IDPContext;
import net.bluemind.dataprotect.service.tool.ToolBootstrap;

public class DPContext implements IDPContext {

	private final IServerTaskMonitor monitor;
	private static final Logger logger = LoggerFactory.getLogger(DPContext.class);

	public DPContext(IServerTaskMonitor monitor) {
		this.monitor = monitor;
	}

	@Override
	public void info(String locale, String msg) {
		if ("en".equals(locale)) {
			logger.info(msg);
		}
		if (monitor != null) {
			monitor.log(msg);
		}
	}

	@Override
	public void warn(String locale, String msg) {
		if ("en".equals(locale)) {
			logger.warn(msg);
		}

		if (monitor != null) {
			monitor.log(msg);
		}
	}

	@Override
	public void error(String locale, String msg) {
		if ("en".equals(locale)) {
			logger.error(msg);
		}

		if (monitor != null) {
			monitor.log(msg);
		}
	}

	@Override
	public ITool tool() {
		return new ToolBootstrap(this);
	}
}
