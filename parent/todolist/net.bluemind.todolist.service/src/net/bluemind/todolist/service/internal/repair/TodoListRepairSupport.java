/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.todolist.service.internal.repair;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.tag.service.TagsSanitizer;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.persistence.VTodoIndexStore;
import net.bluemind.todolist.persistence.VTodoStore;
import net.bluemind.todolist.service.internal.VTodoContainerStoreService;

public class TodoListRepairSupport {

	private final BmContext context;
	private final String parentReportId;

	public TodoListRepairSupport(BmContext context, String parentReportId) {
		this.context = context;
		this.parentReportId = parentReportId;
	}

	public void check(String containerUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		RepairContext ctx = RepairContext.create(context, containerUid);

		if (streamErrors(ctx).anyMatch(e -> e.getKey() == true)) {
			report.ko(parentReportId, String.format("Todolist %s need a repair", containerUid));
		} else {
			report.ok(parentReportId, String.format("Todolist %s is ok", containerUid));
		}
	}

	public void repair(String containerUid, DiagnosticReport report, IServerTaskMonitor monitor) {
		RepairContext ctx = RepairContext.create(context, containerUid);
		long repairCount = streamErrors(ctx).filter(e -> e.getKey() == true).map(e -> e.getValue())
				.map(e -> ctx.vStore.update(e.uid, e.displayName, e.value)).count();

		if (repairCount > 0) {
			report.ok(parentReportId, String.format("Todolist %s repaired", containerUid));
		} else {
			report.ok(parentReportId, String.format("Todolist %s is ok", containerUid));
		}

		reindex(containerUid);
		report.ok(parentReportId, String.format("Todolist %s reindexed", containerUid));
	}

	private Stream<Map.Entry<Boolean, ItemValue<VTodo>>> streamErrors(RepairContext ctx) {
		List<String> all = ctx.vStore.allUids();

		TagsSanitizer tagsSanitizer = new TagsSanitizer(context.su());

		return all.stream().map(uid -> ctx.vStore.get(uid, null)).map(todo -> {
			boolean sanitized = tagsSanitizer.sanitize(todo.value.categories);
			return Maps.immutableEntry(sanitized, todo);
		});
	}

	private void reindex(String containerUid) {
		RepairContext ctx = RepairContext.create(context, containerUid);
		List<String> all = ctx.vStore.allUids();
		VTodoIndexStore indexStore = new VTodoIndexStore(ESearchActivator.getClient(), ctx.container);

		indexStore.deleteAll();

		Lists.partition(all, 500).forEach(uids -> {
			List<ItemValue<VTodo>> values = ctx.vStore.getMultiple(uids);
			indexStore.updates(values);
		});
	}

	private static class RepairContext {
		public final Container container;
		public final VTodoContainerStoreService vStore;

		public RepairContext(VTodoContainerStoreService vStore, Container container) {
			this.vStore = vStore;
			this.container = container;
		}

		public static RepairContext create(BmContext context, String containerUid) {
			DataSource ds = DataSourceRouter.get(context, containerUid);
			ContainerStore cStore = new ContainerStore(context, ds, context.getSecurityContext());
			Container container = cStore.doOrFail(() -> cStore.get(containerUid));
			VTodoStore vtodoStore = new VTodoStore(ds, container);
			VTodoContainerStoreService vStore = new VTodoContainerStoreService(context, ds,
					context.getSecurityContext(), container, ITodoUids.TYPE, vtodoStore);

			RepairContext ctx = new RepairContext(vStore, container);

			return ctx;
		}

	}
}
