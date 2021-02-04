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
package net.bluemind.ui.adminconsole.jobs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.ui.adminconsole.base.Actions;

public class JobExecutionsGrid extends DataGrid<JobExecution> implements IBmGrid<JobExecution> {
	private MultiSelectionModel<JobExecution> selectionModel;
	private ListDataProvider<JobExecution> ldp;

	public JobExecutionsGrid() {
		ProvidesKey<JobExecution> keyProvider = new ProvidesKey<JobExecution>() {
			@Override
			public Object getKey(JobExecution item) {
				return (item == null) ? null : item.id;
			}
		};

		selectionModel = new MultiSelectionModel<JobExecution>(keyProvider);

		IEditHandler<JobExecution> editHandler = new IEditHandler<JobExecution>() {

			@Override
			public SelectAction edit(CellPreviewEvent<JobExecution> cpe) {
				if (cpe.getColumn() == 0) {
					return SelectAction.TOGGLE;
				} else {
					JobExecution j = cpe.getValue();
					Map<String, String> ssr = new HashMap<>();
					ssr.put("exec", j.id + "");
					ssr.put("job", j.jobId);
					ssr.put("activeTab", 2 + "");
					Actions.get().showWithParams2("jobLogsViewer", ssr);
					return SelectAction.IGNORE;
				}
			}
		};

		RowSelectionEventManager<JobExecution> rowSelectionEventManager = RowSelectionEventManager
				.<JobExecution>createRowManager(editHandler);
		setSelectionModel(selectionModel, rowSelectionEventManager);

		Column<JobExecution, Boolean> checkColumn = new Column<JobExecution, Boolean>(new CheckboxCell(true, false)) {
			@Override
			public Boolean getValue(JobExecution de) {
				return selectionModel.isSelected(de);
			}
		};
		Header<Boolean> selHead = new CellHeader<JobExecution>(new CheckboxCell(), this);
		addColumn(checkColumn, selHead, selHead);
		setColumnWidth(checkColumn, 40, Unit.PX);

		Column<JobExecution, TippedResource> typeColumn = new Column<JobExecution, TippedResource>(
				new TooltipedImageCell()) {

			@SuppressWarnings("incomplete-switch")
			@Override
			public TippedResource getValue(JobExecution j) {
				String style = "fa-circle grey";
				String tip = JobTexts.INST.jobNeverExecuted();

				JobExitStatus status = j.status;
				switch (status) {
				case SUCCESS:
					style = "fa-check-circle green";
					break;
				case COMPLETED_WITH_WARNINGS:
					style = "fa-warning job-warning";
					break;
				case FAILURE:
					style = "fa-minus-circle red";
					break;
				}
				tip = JobHelper.i18n(status);
				return new TippedResource(style, tip);
			}
		};
		typeColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		addColumn(typeColumn, "", "");
		setColumnWidth(typeColumn, 32, Unit.PX);

		TextColumn<JobExecution> domain = new TextColumn<JobExecution>() {
			@Override
			public String getValue(JobExecution d) {
				ItemValue<Domain> dom = DomainsHolder.get().getDomainByUid(d.domainUid);
				return dom != null ? dom.value.defaultAlias : d.domainUid;
			}
		};
		addColumn(domain, JobTexts.INST.domain(), JobTexts.INST.domain());
		setColumnWidth(domain, 160, Unit.PX);

		TextColumn<JobExecution> lastRun = new TextColumn<JobExecution>() {
			@Override
			public String getValue(JobExecution d) {
				return DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM).format(d.startDate,
						TimeZone.createTimeZone(0));
			}
		};
		addColumn(lastRun, JobTexts.INST.lastExecution(), JobTexts.INST.lastExecution());
		setColumnWidth(lastRun, 50, Unit.PCT);

		TextColumn<JobExecution> duration = new TextColumn<JobExecution>() {
			@Override
			public String getValue(JobExecution d) {
				long s = d.startDate.getTime();
				long e = d.endDate.getTime();
				long seconds = (e - s) / 1000;
				return JobTexts.INST.seconds("" + seconds);
			}
		};
		addColumn(duration, JobTexts.INST.duration(), JobTexts.INST.duration());
		setColumnWidth(duration, 50, Unit.PCT);

		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);
		setPageSize(Integer.MAX_VALUE);

		this.ldp = new ListDataProvider<JobExecution>();
		ldp.addDataDisplay(this);
	}

	@Override
	public void refresh() {
		ldp.refresh();
	}

	@Override
	public void selectAll(boolean b) {
		for (JobExecution d : getValues()) {
			selectionModel.setSelected(d, b);
		}
	}

	@Override
	public List<JobExecution> getValues() {
		return ldp.getList();
	}

	@Override
	public void setValues(List<JobExecution> values) {
		ldp.setList(values);
		ldp.refresh();
	}

	public HandlerRegistration addSelectionChangeHandler(Handler handler) {
		return selectionModel.addSelectionChangeHandler(handler);
	}

	public List<Integer> getSelected() {
		LinkedList<Integer> ret = new LinkedList<>();
		for (JobExecution je : selectionModel.getSelectedSet()) {
			ret.add(je.id);
		}
		return ret;
	}

}
