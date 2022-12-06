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
package net.bluemind.ui.adminconsole.monitoring.screens;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;

import net.bluemind.system.api.hot.upgrade.HotUpgradeTask;
import net.bluemind.ui.adminconsole.monitoring.l10n.ScreensConstants;

public class HotUpgradeTasksGrid extends DataGrid<HotUpgradeTask> {

	public static DateTimeFormat sdf = DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM);

	private static final ScreensConstants txt = ScreensConstants.INST;

	private ListDataProvider<HotUpgradeTask> ldp;
	private HtmlColumn<HotUpgradeTask> operation;
	private TextColumn<HotUpgradeTask> createdAt;
	private TextColumn<HotUpgradeTask> updatedAt;
	private HtmlColumn<HotUpgradeTask> status;
	private TextColumn<HotUpgradeTask> executionMode;
	private HtmlColumn<HotUpgradeTask> mandatory;

	public HotUpgradeTasksGrid() {
		createColums();
		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);
		setPageSize(Integer.MAX_VALUE);

		this.ldp = new ListDataProvider<HotUpgradeTask>();
		ldp.addDataDisplay(this);

		AsyncHandler columnSortHandler = new AsyncHandler(this);
		addColumnSortHandler(columnSortHandler);

		AsyncDataProvider<HotUpgradeTask> provider = new AsyncDataProvider<HotUpgradeTask>() {

			@Override
			protected void onRangeChanged(HasData<HotUpgradeTask> display) {
				ColumnSortList columnSortList = getColumnSortList();
				if (columnSortList.size() > 0) {
					ColumnSortInfo columnSortInfo = columnSortList.get(0);

					boolean asc = columnSortInfo.isAscending();
					List<HotUpgradeTask> values = getValues();
					if (columnSortInfo.getColumn().equals(operation)) {
						Collections.sort(values, Comparator.comparing(x -> x.operation));
					} else if (columnSortInfo.getColumn().equals(createdAt)) {
						Collections.sort(values, (a, b) -> a.createdAt.compareTo(b.createdAt));
					} else if (columnSortInfo.getColumn().equals(updatedAt)) {
						Collections.sort(values, (a, b) -> a.updatedAt.compareTo(b.updatedAt));
					} else if (columnSortInfo.getColumn().equals(updatedAt)) {
						Collections.sort(values, Comparator.comparing(x -> x.status.name()));
					} else if (columnSortInfo.getColumn().equals(executionMode)) {
						Collections.sort(values, Comparator.comparing(x -> x.executionMode.name()));
					} else if (columnSortInfo.getColumn().equals(mandatory)) {
						Collections.sort(values, Comparator.comparing(x -> x.mandatory));
					}

					if (!asc) {
						Collections.reverse(values);
					}
					setValues(values);
				}
			}
		};
		provider.addDataDisplay(this);
	}

	private void createColums() {

		TextColumn<HotUpgradeTask> id = new TextColumn<HotUpgradeTask>() {
			@Override
			public String getValue(HotUpgradeTask task) {
				return "" + task.id;
			}
		};
		addColumn(id, "ID", "ID");
		setColumnWidth(id, 20, Unit.PCT);

		operation = new HtmlColumn<HotUpgradeTask>() {
			@Override
			public String getValue(HotUpgradeTask task) {
				return "<b>" + task.operation + "</b>";
			}
		};
		addColumn(operation, txt.operation(), txt.operation());
		setColumnWidth(operation, 60, Unit.PCT);
		operation.setSortable(true);

		createdAt = new TextColumn<HotUpgradeTask>() {
			@Override
			public String getValue(HotUpgradeTask task) {
				if (task.createdAt != null) {
					return sdf.format(task.createdAt);
				} else {
					return "";
				}

			}
		};
		addColumn(createdAt, txt.createdAt(), txt.createdAt());
		setColumnWidth(createdAt, 40, Unit.PCT);
		createdAt.setSortable(true);

		updatedAt = new TextColumn<HotUpgradeTask>() {
			@Override
			public String getValue(HotUpgradeTask task) {
				if (task.updatedAt != null) {
					return sdf.format(task.updatedAt);
				} else {
					return "";
				}
			}
		};
		addColumn(updatedAt, txt.updatedAt(), txt.updatedAt());
		setColumnWidth(updatedAt, 40, Unit.PCT);
		updatedAt.setSortable(true);

		status = new HtmlColumn<HotUpgradeTask>() {
			@Override
			public String getValue(HotUpgradeTask task) {
				switch (task.status) {
				case PLANNED:
				case SUCCESS:
					return task.status.name();
				case FAILURE:
					return "<p style=\"color:red\">" + task.status.name() + "</p>";
				}
				return "";
			}
		};
		addColumn(status, txt.status(), txt.status());
		setColumnWidth(status, 25, Unit.PCT);
		status.setSortable(true);

		executionMode = new TextColumn<HotUpgradeTask>() {
			@Override
			public String getValue(HotUpgradeTask task) {
				return task.executionMode.name();
			}
		};
		addColumn(executionMode, txt.executionMode(), txt.executionMode());
		setColumnWidth(executionMode, 40, Unit.PCT);
		executionMode.setSortable(true);

		mandatory = new HtmlColumn<HotUpgradeTask>() {
			@Override
			public String getValue(HotUpgradeTask task) {
				return task.mandatory ? "<b>" + Boolean.toString(task.mandatory) + "</b>"
						: Boolean.toString(task.mandatory);
			}
		};
		addColumn(mandatory, txt.mandatory(), txt.mandatory());
		setColumnWidth(mandatory, 40, Unit.PCT);
		mandatory.setSortable(true);

		HtmlColumn<HotUpgradeTask> events = new HtmlColumn<HotUpgradeTask>() {
			@Override
			public String getValue(HotUpgradeTask task) {
				return task.events.stream().map(e -> {
					String date = "<b>" + sdf.format(new Date(e.date)) + "</b>";
					return date + ": " + e.status.name() + ": " + e.message;
				}).reduce("", (sum, line) -> sum + line + "<br>");
			}
		};
		addColumn(events, txt.events(), txt.events());
		setColumnWidth(events, 100, Unit.PCT);

	}

	public void setValues(List<HotUpgradeTask> entities) {
		ldp.setList(entities);
		ldp.refresh();
	}

	public List<HotUpgradeTask> getValues() {
		return ldp.getList();
	}

	public void refresh() {
		ldp.refresh();
	}

}
