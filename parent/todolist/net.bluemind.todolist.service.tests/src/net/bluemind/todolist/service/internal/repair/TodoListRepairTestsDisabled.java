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

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.tag.api.TagRef;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.VTodo;
import net.bluemind.todolist.service.AbstractServiceTests;
import net.bluemind.todolist.service.internal.repair.TodoListRepairSupport;

public class TodoListRepairTestsDisabled extends AbstractServiceTests {

	@Override
	protected ITodoList getService(SecurityContext context) throws ServerFault {
		return ServerSideServiceProvider.getProvider(defaultSecurityContext).instance(ITodoList.class, container.uid);
	}

	@Test
	public void vtodo_CheckOk() {
		VTodo todo = defaultVTodo(UUID.randomUUID().toString());
		todo.categories = ImmutableList.of(TagRef.create(tagContainer.uid, "tag1", tag1));

		vtodoStoreService.create(todo.uid, todo.summary, todo);

		BmTestContext context = new BmTestContext(defaultSecurityContext);
		TodoListRepairSupport rs = new TodoListRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.check(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void vtodo_RepairOk() {
		VTodo todo = defaultVTodo(UUID.randomUUID().toString());
		todo.categories = ImmutableList.of(TagRef.create(tagContainer.uid, "tag1", tag1));

		vtodoStoreService.create(todo.uid, todo.summary, todo);

		BmTestContext context = new BmTestContext(defaultSecurityContext);
		TodoListRepairSupport rs = new TodoListRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.repair(container.uid, report, new NullTaskMonitor());
		assertEquals(2, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(String.format("Todolist %s is ok", container.uid), report.entries.get(0).message);
		assertEquals("reportId", report.entries.get(1).id);
		assertEquals(String.format("Todolist %s reindexed", container.uid), report.entries.get(1).message);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);
	}

	@Test
	public void tags_CheckNeedRepair() {
		VTodo todo = defaultVTodo(UUID.randomUUID().toString());
		todo.categories = ImmutableList.of(TagRef.create(tagContainer.uid, "tag1", "toRepair", "toRepair"));

		vtodoStoreService.create(todo.uid, todo.summary, todo);

		BmTestContext context = new BmTestContext(defaultSecurityContext);
		TodoListRepairSupport rs = new TodoListRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.check(container.uid, report, new NullTaskMonitor());
		assertEquals(1, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(DiagnosticReport.State.KO, report.entries.get(0).state);
	}

	@Test
	public void tags_Repair() {
		VTodo todo = defaultVTodo(UUID.randomUUID().toString());
		todo.categories = ImmutableList.of(TagRef.create(tagContainer.uid, "tag1", "toRepair", "toRepair"));

		vtodoStoreService.create(todo.uid, todo.summary, todo);

		BmTestContext context = new BmTestContext(defaultSecurityContext);
		TodoListRepairSupport rs = new TodoListRepairSupport(context, "reportId");
		DiagnosticReport report = DiagnosticReport.create();
		rs.repair(container.uid, report, new NullTaskMonitor());
		assertEquals(2, report.entries.size());
		assertEquals("reportId", report.entries.get(0).id);
		assertEquals(String.format("Todolist %s repaired", container.uid), report.entries.get(0).message);
		assertEquals("reportId", report.entries.get(1).id);
		assertEquals(String.format("Todolist %s reindexed", container.uid), report.entries.get(1).message);
		assertEquals(DiagnosticReport.State.OK, report.entries.get(0).state);

		ItemValue<VTodo> vtodo = vtodoStoreService.get(todo.uid, null);
		TagRef tagRef = vtodo.value.categories.get(0);
		assertEquals(tag1.color, tagRef.color);
		assertEquals(tag1.label, tagRef.label);

	}

	protected VTodo defaultVTodo(String uid) {

		VTodo todo = new VTodo();
		todo.uid = uid;
		DateTimeZone tz = DateTimeZone.UTC;
		DateTime temp = new DateTime(2024, 12, 28, 0, 0, 0, tz);
		todo.dtstart = BmDateTimeWrapper.create(temp, Precision.DateTime);
		todo.due = BmDateTimeWrapper.create(temp.plusMonths(1), Precision.DateTime);
		todo.summary = "Test Todo";
		todo.location = "Toulouse";
		todo.description = "Lorem ipsum";
		todo.priority = 3;
		todo.percent = 0;
		return todo;

	}

}
