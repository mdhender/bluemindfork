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

public class DelegateTaskMonitor extends AbstractTaskMonitor {

	private final IServerTaskMonitor delegate;

	public DelegateTaskMonitor(IServerTaskMonitor delegate, int depth) {
		super(depth);
		this.delegate = delegate;
	}

	@Override
	public void begin(double totalWork, String log) {
		delegate.begin(totalWork, log);
	}

	@Override
	public void progress(double doneWork, String log) {
		delegate.progress(doneWork, log);
	}

	@Override
	public void end(boolean success, String log, String result) {
		delegate.end(success, log, result);
	}

	@Override
	public void log(String log) {
		delegate.log(log);
	}
}
