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

import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.PartGeneration;

public class InstallTask implements IServerTask {

	private final DataProtectGeneration gen;
	private final BmContext ctx;

	public InstallTask(BmContext ctx, DataProtectGeneration gen) {
		this.gen = gen;
		this.ctx = ctx;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		List<PartGeneration> parts = gen.parts;
		PartGeneration pgPart = null;
		for (PartGeneration part : parts) {
			if ("bm/pgsql".equals(part.tag)) {
				pgPart = part;
				break;
			}
		}
		if (pgPart == null) {
			throw new ServerFault("PG part is missing from generation " + gen.id);
		}
		// target the main database
		PRA lgt = new PRA(ctx, gen, pgPart, parts, "bj");
		ITasksManager mgr = ctx.provider().instance(ITasksManager.class);
		TaskRef pgRestore = mgr.run(lgt);
		ITask taskApi = ctx.provider().instance(ITask.class, "" + pgRestore.id);
		TaskStatus status = taskApi.status();
		monitor.subWork(status.steps);
		while (!status.state.ended) {
			monitor.progress(status.progress, status.lastLogEntry);
			status = taskApi.status();
		}

		monitor.end(true, "installed", "{ \"status\": \"ok\" }");
	}
}
