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
package net.bluemind.system.schemaupgrader.tests.internal;

import net.bluemind.core.task.service.IServerTaskMonitor;

public class TestMonitor implements IServerTaskMonitor {

	@Override
	public IServerTaskMonitor subWork(double work) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IServerTaskMonitor subWork(String logPrefix, double work) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void begin(double totalWork, String log) {
		// TODO Auto-generated method stub

	}

	@Override
	public void progress(double doneWork, String log) {
		// TODO Auto-generated method stub

	}

	@Override
	public void end(boolean success, String log, String result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void log(String log) {
		System.out.println(log);
		// TODO Auto-generated method stub

	}

}
