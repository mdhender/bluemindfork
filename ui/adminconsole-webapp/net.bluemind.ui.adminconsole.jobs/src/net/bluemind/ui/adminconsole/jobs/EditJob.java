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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.scheduledjob.api.Job;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.JobKind;
import net.bluemind.scheduledjob.api.JobPlanification;
import net.bluemind.scheduledjob.api.PlanKind;
import net.bluemind.scheduledjob.api.gwt.endpoint.JobGwtEndpoint;
import net.bluemind.ui.admin.client.forms.TextEdit;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.IDomainChangedListener;
import net.bluemind.ui.adminconsole.base.ui.CrudActionBar;
import net.bluemind.ui.adminconsole.base.ui.ResetReply;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.StringEdit;

public class EditJob extends Composite implements IStatusFilterListener, IDomainChangedListener, IGwtScreenRoot {

	private static final JobGwtEndpoint jobApi = new JobGwtEndpoint(Ajax.TOKEN.getSessionId());

	public static final String TYPE = "bm.ac.EditJob";

	private static EditJobUiBinder uib = GWT.create(EditJobUiBinder.class);

	interface EditJobUiBinder extends UiBinder<DockLayoutPanel, EditJob> {

	}

	private DockLayoutPanel dlp;

	@UiField
	Label title;

	@UiField
	StringEdit jid;

	@UiField
	TextEdit desc;

	@UiField
	CrudActionBar actionBar;

	@UiField
	DockLayoutPanel execDlp;

	@UiField
	Button deleteExecutions;

	@UiField
	JobExecutionsGrid execGrid;

	@UiField
	TabLayoutPanel tabPanel;

	@UiField
	StatusFilter statusFilter;

	@UiField
	HTMLPanel plansPanel;

	@UiField
	CheckBox reportEnable;

	@UiField
	StringEdit reportRecipients;

	private Job edited;
	private List<PlanificationEditor> planEditors;

	private HandlerRegistration gridSelReg;

	private String domain;

	private ScreenRoot instance;

	public EditJob(ScreenRoot screenRoot) {
		this.instance = screenRoot;
		this.dlp = uib.createAndBindUi(this);
		initWidget(dlp);
		dlp.setHeight("100%");

		actionBar.setSaveAction(new ScheduledCommand() {
			@Override
			public void execute() {
				save();
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {
			@Override
			public void execute() {
				Actions.get().showWithParams2("jobsManager", new HashMap<String, String>());
			}
		});

		tabPanel.addSelectionHandler(new SelectionHandler<Integer>() {
			@Override
			public void onSelection(SelectionEvent<Integer> event) {
				if (event.getSelectedItem() == 2) {
					execGrid.refresh();
				}
			}
		});
		// Register extensions points here
	}

	public void construct() {
		jid.setId("edit-job-id");
		reportRecipients.setId("edit-job-recipients");
	}

	protected void onScreenShown(Map<String, String> ssr) {
		this.planEditors = new LinkedList<PlanificationEditor>();
		String jobId = ssr.get("jobId");
		Integer activeTab = Integer.parseInt(ssr.get("activeTab"));
		this.domain = ssr.get("domain");
		if (jobId == null) {
			GWT.log("null job in request", new Throwable());
			// AdminCtrl.get().showScreenComplete();
		} else {
			load(jobId, domain, activeTab, true);
		}
		statusFilter.addListener(this);
		DomainsHolder.get().registerDomainChangedListener(this);

		Handler handler = new Handler() {

			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				List<Integer> selection = execGrid.getSelected();
				deleteExecutions.setEnabled(!selection.isEmpty());
			}
		};
		this.gridSelReg = execGrid.addSelectionChangeHandler(handler);
	}

	@UiHandler("deleteExecutions")
	void deleteExecClicked(ClickEvent ce) {
		jobApi.deleteExecutions(execGrid.getSelected(), new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				deleteExecutions.setEnabled(false);
				load(edited.id, domain, tabPanel.getSelectedIndex(), false);
			}

			@Override
			public void failure(Throwable e) {
			}
		});
	}

	private void load(final String jobId, String domain, final Integer activeTab, final boolean firstTime) {
		if (null == domain && !DomainsHolder.get().getSelectedDomain().uid.equals("global.virt")) {
			domain = DomainsHolder.get().getSelectedDomain().uid;
		}
		this.domain = domain;
		JobExecutionQuery jeq = new JobExecutionQuery();
		jeq.jobId = jobId;
		if (domain != null) {
			jeq.domain = domain;
		}
		jeq.statuses = statusFilter.getAcceptedStatus();

		jeq.from = 0;
		jeq.size = 20;

		jobApi.searchExecution(jeq, new AsyncHandler<ListResult<JobExecution>>() {
			@Override
			public void failure(Throwable e) {
			}

			@Override
			public void success(final ListResult<JobExecution> execs) {
				jobApi.getJobFromId(jobId, new AsyncHandler<Job>() {

					@Override
					public void success(Job value) {
						GWT.log("*** Found job with " + value.domainPlanification.size() + " plans.");
						updateUi(value, execs);

						if (activeTab != null) {
							tabPanel.selectTab(activeTab);
						}

						// if (firstTime) {
						// AdminCtrl.get().showScreenComplete();
						// }
					}

					@Override
					public void failure(Throwable e) {
					}
				});

			}

		});

	}

	private void updateUi(Job job, ListResult<JobExecution> execs) {
		edited = job;

		title.setText(getTexts().title(JobHelper.getShortId(edited)));
		jid.setStringValue(edited.id);
		desc.setStringValue(edited.description);

		reportEnable.setValue(edited.sendReport);
		reportRecipients.setStringValue(edited.recipients);

		execGrid.setValues(execs.values);
		Collection<ItemValue<Domain>> domains = DomainsHolder.get().getDomains();
		Map<String, ItemValue<Domain>> domsIdx = new HashMap<>();
		for (ItemValue<Domain> div : domains) {
			domsIdx.put(div.uid, div);
		}
		Map<String, JobPlanification> plans = new HashMap<>();
		for (JobPlanification domplan : job.domainPlanification) {
			GWT.log("On plan for " + domplan.domain + ": " + domplan.kind);
			plans.put(domplan.domain, domplan);
		}

		if (job.kind == JobKind.GLOBAL) {
			domains = new LinkedList<>();
			for (ItemValue<Domain> div : DomainsHolder.get().getDomains()) {
				if (div.value.global) {
					domains.add(div);
				}
			}
		} else {
			domains = Arrays.asList(domsIdx.get(domain));
		}
		clearPlanEditors();
		for (ItemValue<Domain> d : domains) {
			PlanificationEditor pe = new PlanificationEditor();
			pe.setDomain(d.value);
			JobPlanification plan = plans.get(d.uid);
			if (plan == null) {
				GWT.log("Job has no plan set for " + d.uid);
				plan = new JobPlanification();
				plan.kind = (PlanKind.OPPORTUNISTIC);
				plan.domain = d.value.name;
			} else {
				GWT.log("Job has a plan " + plan.kind);
			}
			pe.setJobPlanification(plan);
			planEditors.add(pe);
			plansPanel.add(pe);
		}
	}

	private void clearPlanEditors() {
		Iterator<PlanificationEditor> it = planEditors.iterator();
		while (it.hasNext()) {
			PlanificationEditor pe = it.next();
			pe.destroy();
			it.remove();
		}
		plansPanel.clear();
	}

	private void save() {
		try {
			Job j = new Job();
			j.id = edited.id;
			j.kind = edited.kind;
			j.description = "";

			j.sendReport = reportEnable.getValue();
			j.recipients = reportRecipients.getStringValue();

			for (PlanificationEditor pe : planEditors) {
				JobPlanification jpl = pe.getJobPlanification();
				j.domainPlanification.add(jpl);
			}
			jobApi.update(j, new AsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					// TODO Auto-generated method stub
					Actions.get().showWithParams2("jobsManager", new HashMap<String, String>());
				}

				@Override
				public void failure(Throwable e) {
					// TODO Auto-generated method stub

				}
			});
		} catch (RecValidityException rve) {
			GWT.log(rve.getMessage(), rve);
			// Actions.get().showError(JobTexts.INST.invalidPlanification());
		}
	}

	protected ResetReply reset() {
		clearPlanEditors();
		statusFilter.removeListener(this);
		DomainsHolder.get().removeDomainChangedListener(this);
		gridSelReg.removeHandler();
		gridSelReg = null;
		return ResetReply.OK;
	}

	@UiFactory
	JobTexts getTexts() {
		return JobTexts.INST;
	}

	@Override
	public void activeDomainChanged(ItemValue<Domain> newActiveDomain) {
		load(edited.id, newActiveDomain.uid, tabPanel.getSelectedIndex(), false);
	}

	@Override
	public void filteredStatusChanged(Set<JobExitStatus> status, String filter) {
		load(edited.id, domain, tabPanel.getSelectedIndex(), false);
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new EditJob(screenRoot);
			}
		});
	}

	@Override
	public void attach(Element e) {
		DOM.appendChild(e, getElement());
		JsMapStringString state = instance.getState();
		Map<String, String> ssr = new HashMap<>();
		for (int i = 0; i < state.keys().length(); i++) {
			String k = state.keys().get(i);
			String v = state.get(k);
			ssr.put(k, v);
		}
		onScreenShown(ssr);
		onAttach();
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doLoad(ScreenRoot instance) {
		// TODO Auto-generated method stub

	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("editJob", TYPE).cast();
		return screenRoot;
	}

}
