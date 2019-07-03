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

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.domain.api.Domain;
import net.bluemind.scheduledjob.api.JobPlanification;
import net.bluemind.scheduledjob.api.PlanKind;

public class PlanificationEditor extends Composite {

	private static PlanificationEditorUiBinder uiBinder = GWT.create(PlanificationEditorUiBinder.class);

	interface PlanificationEditorUiBinder extends UiBinder<HTMLPanel, PlanificationEditor> {
	}

	@UiField
	Label domainName;

	@UiField
	StringListBox planKind;

	@UiField
	HTMLPanel planForm;

	private HandlerRegistration pkReg;

	private JobRecEditor jrEditor;

	private PlanKind kind;

	private Domain domain;

	public PlanificationEditor() {
		initWidget(uiBinder.createAndBindUi(this));

		planKind.addItem(PlanKind.OPPORTUNISTIC.name(), JobTexts.INST.opportunisticPlan());
		planKind.addItem(PlanKind.SCHEDULED.name(), JobTexts.INST.scheduledPlan());
		planKind.addItem(PlanKind.DISABLED.name(), JobTexts.INST.disabledPlan());

		select(PlanKind.OPPORTUNISTIC, false);
		pkReg = planKind.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				select(PlanKind.valueOf(planKind.getSelected()), false);
			}
		});
	}

	private void select(PlanKind pk, boolean updateList) {
		planForm.clear();
		if (jrEditor != null) {
			jrEditor.destroy();
			jrEditor = null;
		}
		this.kind = pk;
		GWT.log("pk: " + pk + " " + (domain != null ? domain.name : "domain not set yet"));
		switch (pk) {
		case OPPORTUNISTIC:
			if (updateList) {
				planKind.setSelectedIndex(0);
			}
			planForm.add(new Label(JobTexts.INST.opportunisticDesc()));
			break;
		case SCHEDULED:
			if (updateList) {
				planKind.setSelectedIndex(1);
			}
			jrEditor = new JobRecEditor();
			planForm.add(jrEditor);
			break;
		case DISABLED:
			if (updateList) {
				planKind.setSelectedIndex(2);
			}
			planForm.add(new Label(JobTexts.INST.disabledDesc()));
			break;
		}

	}

	public void setJobPlanification(JobPlanification jp) {
		if (jp == null || jp.kind == null) {
			GWT.log("null job plan: " + jp);
		}
		select(jp.kind, true);
		if (jrEditor != null) {
			jrEditor.setRec(jp.rec);
		}
	}

	public void setDomain(Domain schedDomain) {
		domainName.setText(schedDomain.name);
		this.domain = schedDomain;
	}

	public JobPlanification getJobPlanification() throws RecValidityException {
		JobPlanification jp = new JobPlanification();
		jp.kind = kind;
		if (jrEditor != null) {
			jp.rec = jrEditor.getRec();
		}
		jp.domain = domain.name;
		return jp;
	}

	public Domain getDomain() {
		return null;
	}

	public void destroy() {
		pkReg.removeHandler();
		pkReg = null;
	}
}
