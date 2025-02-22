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
package net.bluemind.ui.adminconsole.dataprotect;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;

import net.bluemind.dataprotect.api.RestoreOperation;

public abstract class ActionHandler<T> {

	private String name;
	private T object;

	ActionHandler(String name, T object) {
		this.name = name;
		this.object = object;
	}

	public T getObject() {
		return object;
	}

	public String getName() {
		return name;
	}

	public abstract void handle();

	public abstract RestoreOperation getRestoreOp();

	public abstract ScheduledCommand getCommand();

}
