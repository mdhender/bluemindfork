/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.maintenance.elastic;

import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.ITask;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.mailbox.api.IMailboxMgmt;
import net.bluemind.system.api.hot.upgrade.HotUpgradeTask;
import net.bluemind.system.upgraders.hot.service.HotUpgradeOperation;

public class ElasticAliasMoveOperation implements HotUpgradeOperation {
	public static final String OP_NAME = "elasticAliasMove";

	private final BmContext context;

	public ElasticAliasMoveOperation(BmContext context) {
		this.context = context;
	}

	@Override
	public void execute(HotUpgradeTask task, IServerTaskMonitor monitor, DiagnosticReport report) throws Exception {

		monitor.begin(1, "start " + task.parameters);

		IMailboxMgmt mboxMgmt = context.provider().instance(IMailboxMgmt.class, task.getParameterAsString("domainUid"));
		TaskRef moveTask = mboxMgmt.moveIndex(task.getParameterAsString("mailboxUid"),
				task.getParameterAsString("targetIndex"), false);
		monitor.log("Forwarding progress of alias move " + moveTask.id);
		ITask tsk = context.provider().instance(ITask.class, moveTask.id);
		TaskUtils.forwardProgress(tsk, monitor.subWork(1));

		boolean remove = "true".equals(task.getParameterAsString("tryRemove"));
		if (remove) {
			String source = task.getParameterAsString("sourceIndex");
			IMailboxMgmt mboxMgmtAdm = context.provider().instance(IMailboxMgmt.class, "global.virt");
			int activeAliases = mboxMgmtAdm.getLiteStats().stream().filter(s -> s.indexName.equals(source))
					.map(s -> s.mailboxes.size()).findAny().orElse(0);
			if (activeAliases == 0) {
				try {
					AcknowledgedResponse delRes = ESearchActivator.getClient().admin().indices()
							.delete(new DeleteIndexRequest(source)).get(10, TimeUnit.MINUTES);
					monitor.log("Deletion of {}: {}", source, delRes.toString());
				} catch (Exception e) { // NOSONAR
					monitor.log("Error droping " + source, e);
				}
			} else {
				monitor.log("We can't remove " + source + " as it has active " + activeAliases + " aliases");
			}
		}

		monitor.end(true, "finished.", "OK");

	}

	@Override
	public String name() {
		return OP_NAME;
	}

	public static class Factory implements HotUpgradeOperation.Factory {

		@Override
		public HotUpgradeOperation create(BmContext context) {
			return new ElasticAliasMoveOperation(context);
		}

	}

}
