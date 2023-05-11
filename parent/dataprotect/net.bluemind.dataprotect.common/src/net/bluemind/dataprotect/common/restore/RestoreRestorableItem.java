/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.dataprotect.common.restore;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.container.model.ContainerUpdatesResult.InError;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.action.EmailData;
import net.bluemind.dataprotect.service.action.RestoreActionExecutor;

public class RestoreRestorableItem {

	public Restorable item;
	public RestoreActionExecutor<EmailData> executor;
	public List<InError> errors = new ArrayList<>();
	public IServerTaskMonitor monitor;

	public RestoreRestorableItem(Restorable item, RestoreActionExecutor<EmailData> executor,
			IServerTaskMonitor monitor) {
		this.item = item;
		this.executor = executor;
		this.monitor = monitor;
	}

	public RestoreRestorableItem(Restorable item, IServerTaskMonitor monitor) {
		this.item = item;
		this.monitor = monitor;
	}

	public RestoreRestorableItem(Restorable item) {
		this.item = item;
		this.monitor = null;
	}

	public String domain() {
		return item.domainUid;
	}

	public String entryUid() {
		return item.entryUid;
	}

	public String liveEntryUid() {
		return item.liveEntryUid();
	}

	public void endTask() {
		if (!errors.isEmpty()) {
			errors.forEach(e -> monitor.error("Error {} for uid {}: {}", e.errorCode, e.uid, e.message));
			monitor.end(false, "finished with errors", "[]");
		} else {
			monitor.end(true, "finished.", "[]");
		}
	}

	public void setMonitor(IServerTaskMonitor monitor) {
		this.monitor = monitor;
	}
}
