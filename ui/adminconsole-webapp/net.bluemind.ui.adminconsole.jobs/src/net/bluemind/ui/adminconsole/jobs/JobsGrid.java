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

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobDomainStatus;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.PlanKind;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;

public class JobsGrid extends DataGrid<Job> implements IBmGrid<Job> {

	private MultiSelectionModel<Job> selectionModel;
	private ListDataProvider<Job> ldp;
	private ProvidesKey<Job> keyProvider;
	private List<JobExecution> active;

	private void showLiveLogs(Job j) {

		JobExecution je = new JobExecution();
		je.domainUid = j.domainPlanification.iterator().next().domain;
		Actions.get().showWithParams2("liveLogsViewer",
				MapBuilder.of("jobId", "" + j.id, "domain", j.domainPlanification.iterator().next().domain));
	}

	public JobsGrid() {
		this.keyProvider = new ProvidesKey<Job>() {
			@Override
			public Object getKey(Job item) {
				if (item == null) {
					return null;
				}
				String d = item.domainPlanification.iterator().next().domain;
				return "" + d + "-" + item.id;
			}
		};
		selectionModel = new MultiSelectionModel<Job>(keyProvider);
		this.getElement().getStyle().setCursor(Cursor.POINTER);

		IEditHandler<Job> editHandler = new IEditHandler<Job>() {

			@Override
			public SelectAction edit(CellPreviewEvent<Job> cpe) {
				int col = cpe.getColumn();
				GWT.log("in edit handler for column " + col);
				if (col == 0) {
					return SelectAction.TOGGLE;
				} else if (col == 2) {
					GWT.log("click on column 2");
					Job j = cpe.getValue();
					showLiveLogs(j);
					return SelectAction.IGNORE;
				} else {
					Job j = cpe.getValue();
					ScreenShowRequest ssr = new ScreenShowRequest();
					ssr.put("job", j);
					Actions.get().showWithParams2("editJob", MapBuilder.of("jobId", j.id, "domain",
							j.domainPlanification.iterator().next().domain, "activeTab", "0"));
					return SelectAction.IGNORE;
				}
			}
		};

		RowSelectionEventManager<Job> rowSelectionEventManager = RowSelectionEventManager
				.<Job>createRowManager(editHandler);
		setSelectionModel(selectionModel, rowSelectionEventManager);

		Column<Job, Boolean> checkColumn = new Column<Job, Boolean>(new CheckboxCell(true, false)) {
			@Override
			public Boolean getValue(Job de) {
				return selectionModel.isSelected(de);
			}
		};
		Header<Boolean> selHead = new CellHeader<Job>(new CheckboxCell(), this);
		addColumn(checkColumn, selHead, selHead);
		setColumnWidth(checkColumn, 40, Unit.PX);

		Column<Job, TippedResource> typeColumn = new Column<Job, TippedResource>(new TooltipedImageCell()) {

			@SuppressWarnings("incomplete-switch")
			@Override
			public TippedResource getValue(Job j) {
				String style = "fa-circle grey";
				String tip = JobTexts.INST.jobNeverExecuted();

				if (!j.domainStatus.isEmpty()) {
					JobDomainStatus status = j.domainStatus.iterator().next();
					switch (status.status) {
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
					tip = JobHelper.i18n(status.status);
				}
				return new TippedResource(style, tip);
			}
		};
		typeColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		addColumn(typeColumn, "", "");
		setColumnWidth(typeColumn, 32, Unit.PX);

		Column<Job, TippedResource> logsColumn = new Column<Job, TippedResource>(new TooltipedImageCell()) {

			@Override
			public TippedResource getValue(Job j) {
				String style = "fa-sticky-note-o";
				String tip = JobTexts.INST.viewLogs();

				return new TippedResource(style, tip);
			}
		};
		logsColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		addColumn(logsColumn, "", "");
		setColumnWidth(logsColumn, 32, Unit.PX);

		TextColumn<Job> name = new TextColumn<Job>() {
			@Override
			public String getValue(Job j) {
				return JobHelper.getShortId(j);
			}
		};
		addColumn(name, JobTexts.INST.jobId(), JobTexts.INST.jobId());
		setColumnWidth(name, 150, Unit.PX);

		TextColumn<Job> type = new TextColumn<Job>() {
			@Override
			public String getValue(Job d) {
				switch (d.kind) {
				case GLOBAL:
					return JobTexts.INST.globalJobKind();
				default:
				case MULTIDOMAIN:
					return JobTexts.INST.multidomainJobKind();
				}
			}
		};
		addColumn(type, JobTexts.INST.jobKind(), JobTexts.INST.jobKind());
		setColumnWidth(type, 100, Unit.PX);

		TextColumn<Job> domain = new TextColumn<Job>() {
			@Override
			public String getValue(Job d) {
				if (d.domainPlanification.isEmpty()) {
					return "--";
				} else {
					String domainUid = d.domainPlanification.iterator().next().domain;
					ItemValue<Domain> dom = DomainsHolder.get().getDomainByUid(domainUid);
					return dom != null ? dom.value.defaultAlias : domainUid;
				}
			}
		};
		addColumn(domain, JobTexts.INST.domain(), JobTexts.INST.domain());
		setColumnWidth(domain, 160, Unit.PX);

		TextColumn<Job> scheduling = new TextColumn<Job>() {
			@Override
			public String getValue(Job d) {
				if (d.domainPlanification.isEmpty()) {
					return JobTexts.INST.opportunisticPlan();
				} else {
					PlanKind kind = d.domainPlanification.iterator().next().kind;
					switch (kind) {
					case DISABLED:
						return JobTexts.INST.disabledPlan();
					case SCHEDULED:
						return JobTexts.INST.scheduledPlan();
					default:
					case OPPORTUNISTIC:
						return JobTexts.INST.opportunisticPlan();
					}
				}
			}
		};
		addColumn(scheduling, JobTexts.INST.planification(), JobTexts.INST.planification());
		setColumnWidth(scheduling, 80, Unit.PX);

		TextColumn<Job> lastRun = new TextColumn<Job>() {
			@Override
			public String getValue(Job d) {
				if (d.domainPlanification.isEmpty()) {
					return "--";
				} else {
					Date da = d.domainPlanification.iterator().next().lastRun;
					if (da != null) {
						return DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM).format(da,
								TimeZone.createTimeZone(0));
					} else {
						return "--";
					}
				}
			}
		};
		addColumn(lastRun, JobTexts.INST.lastExecution(), JobTexts.INST.lastExecution());
		setColumnWidth(lastRun, 20, Unit.PCT);

		TextColumn<Job> nextRun = new TextColumn<Job>() {
			@Override
			public String getValue(Job d) {
				if (d.domainPlanification.isEmpty()) {
					return "--";
				} else {
					Date da = d.domainPlanification.iterator().next().nextRun;
					if (da != null) {
						return DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM).format(da,
								TimeZone.createTimeZone(0));
					} else {
						return "--";
					}
				}
			}
		};
		addColumn(nextRun, JobTexts.INST.nextExecution(), JobTexts.INST.nextExecution());
		setColumnWidth(nextRun, 20, Unit.PCT);

		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);
		setPageSize(Integer.MAX_VALUE);

		this.ldp = new ListDataProvider<Job>();
		ldp.addDataDisplay(this);
	}

	public void setValues(List<Job> entities) {
		ldp.setList(entities);
		ldp.refresh();
	}

	public List<Job> getValues() {
		return ldp.getList();
	}

	public void refresh() {
		ldp.refresh();
	}

	public void selectAll(boolean checked) {
		for (Job d : getValues()) {
			selectionModel.setSelected(d, checked);
		}
	}

	public HandlerRegistration addSelectionChangeHandler(Handler handler) {
		return selectionModel.addSelectionChangeHandler(handler);
	}

	public Collection<Job> getSelected() {
		return selectionModel.getSelectedSet();
	}

	public void setSelected(Collection<Job> jobs) {
		for (Job job : jobs) {
			selectionModel.setSelected(job, true);
		}
	}

	@Override
	public ProvidesKey<Job> getKeyProvider() {
		return keyProvider;
	}

	public void setActiveJobs(List<JobExecution> activeJobs) {
		this.active = activeJobs;
	}

	public List<JobExecution> getActive() {
		return active;
	}

}
