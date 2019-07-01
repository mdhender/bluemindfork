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
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.scheduledjob.api.JobRec;

public class JobRecEditor extends Composite {

	private static final JobRecEditorUiBinder uib = GWT.create(JobRecEditorUiBinder.class);

	interface JobRecEditorUiBinder extends UiBinder<HTMLPanel, JobRecEditor> {

	}

	// periodic req form fields

	@UiField
	RadioButton periodicMin;

	@UiField
	HTMLPanel periodicMinForm;

	@UiField
	StringListBox perMinFreq;

	// periodic req form fields

	@UiField
	RadioButton periodicHour;

	@UiField
	HTMLPanel periodicHourForm;

	@UiField
	StringListBox perHourFreq;

	// Daily req form fields

	@UiField
	RadioButton daily;

	@UiField
	HTMLPanel dailyForm;

	@UiField
	CheckBox dayMon;

	@UiField
	CheckBox dayTue;

	@UiField
	CheckBox dayWed;

	@UiField
	CheckBox dayThu;

	@UiField
	CheckBox dayFri;

	@UiField
	CheckBox daySat;

	@UiField
	CheckBox daySun;

	@UiField
	TextBox dayHour;

	@UiField
	TextBox dayMinutes;

	// other fields

	HTMLPanel activeForm;

	private RecModel recModel;

	private RadioGroup group;

	public JobRecEditor() {
		this.group = new RadioGroup();
		initWidget(uib.createAndBindUi(this));

		fillMinCombo();
		fillHourCombo();

		periodicMin.setValue(true);
		recModel = new RecModel(JobRec.EVERY_MINUTE);
		activeForm = periodicMinForm;
	}

	private void fillHourCombo() {
		perHourFreq.addItem("1", "1");
		perHourFreq.addItem("2", "2");
		perHourFreq.addItem("3", "3");
		perHourFreq.addItem("4", "4");
		perHourFreq.addItem("6", "6");
		perHourFreq.addItem("8", "8");
		perHourFreq.addItem("12", "12");
	}

	private void fillMinCombo() {
		perMinFreq.addItem("1", "1");
		perMinFreq.addItem("2", "2");
		perMinFreq.addItem("5", "5");
		perMinFreq.addItem("10", "10");
		perMinFreq.addItem("15", "15");
		perMinFreq.addItem("20", "20");
		perMinFreq.addItem("30", "30");
	}

	@UiHandler("periodicMin")
	void periodicMinValueChanged(ValueChangeEvent<Boolean> vce) {
		if (vce.getValue()) {
			activeForm.setVisible(false);
			activeForm = periodicMinForm;
			activeForm.setVisible(true);
		}
	}

	@UiHandler("periodicHour")
	void periodicHourValueChanged(ValueChangeEvent<Boolean> vce) {
		if (vce.getValue()) {
			activeForm.setVisible(false);
			activeForm = periodicHourForm;
			activeForm.setVisible(true);
		}
	}

	@UiHandler("daily")
	void dailyValueChanged(ValueChangeEvent<Boolean> vce) {
		if (vce.getValue()) {
			activeForm.setVisible(false);
			activeForm = dailyForm;
			activeForm.setVisible(true);
		}
	}

	public JobRec getRec() throws RecValidityException {
		JobRec jr = new JobRec();
		updateRecModel();
		String cs = recModel.getCronString();
		jr.cronString = cs;
		GWT.log("create cron string: '" + cs + "'");
		return jr;
	}

	private void updateRecModel() throws RecValidityException {
		if (activeForm == periodicMinForm) {
			try {
				int min = Integer.parseInt(perMinFreq.getSelectedValue());
				recModel.setMinutes(min == 1 ? "*" : "*/" + min);
			} catch (Throwable t) {
				throw new RecValidityException();
			}
			recModel.setDaysOfWeek("*");
			recModel.setDaysOfMonth("?");
			recModel.setYear("*");
			recModel.setHours("*");
		} else if (activeForm == periodicHourForm) {
			try {
				int hour = Integer.parseInt(perHourFreq.getSelectedValue());
				recModel.setHours(hour == 1 ? "*" : "*/" + hour);
			} catch (Throwable t) {
				throw new RecValidityException();
			}
			recModel.setMinutes("0");
			recModel.setDaysOfWeek("*");
			recModel.setDaysOfMonth("?");
			recModel.setYear("*");
		} else if (activeForm == dailyForm) {
			StringBuilder days = new StringBuilder();
			if (dayMon.getValue()) {
				if (days.length() > 0) {
					days.append(",");
				}
				days.append("MON");
			}
			if (dayTue.getValue()) {
				if (days.length() > 0) {
					days.append(",");
				}
				days.append("TUE");
			}
			if (dayWed.getValue()) {
				if (days.length() > 0) {
					days.append(",");
				}
				days.append("WED");
			}
			if (dayThu.getValue()) {
				if (days.length() > 0) {
					days.append(",");
				}
				days.append("THU");
			}
			if (dayFri.getValue()) {
				if (days.length() > 0) {
					days.append(",");
				}
				days.append("FRI");
			}
			if (daySat.getValue()) {
				if (days.length() > 0) {
					days.append(",");
				}
				days.append("SAT");
			}
			if (daySun.getValue()) {
				if (days.length() > 0) {
					days.append(",");
				}
				days.append("SUN");
			}
			if (days.length() == 0) {
				throw new RecValidityException("One day must be selected");
			}

			recModel.setDaysOfWeek(days.toString());
			recModel.setDaysOfMonth("?");
			recModel.setYear("*");
			int hour = 0;
			int minute = 0;
			try {
				hour = Integer.parseInt(dayHour.getText());
				if (hour < 0 || hour > 23) {
					throw new RecValidityException("Invalid hour range");
				}
				recModel.setHours(hour + "");
			} catch (RecValidityException re) {
				throw re;
			} catch (Exception e) {
				throw new RecValidityException(e.getMessage());
			}
			try {
				minute = Integer.parseInt(dayMinutes.getText());
				if (minute < 0 || minute > 59) {
					throw new RecValidityException("Invalid minute range");
				}
				recModel.setMinutes(minute + "");
			} catch (RecValidityException re) {
				throw re;
			} catch (Exception e) {
				throw new RecValidityException(e.getMessage());
			}
		} else {
			GWT.log("Can't figure out rec kind from activeForm", new Throwable());
		}
	}

	public void setRec(JobRec rec) {
		this.recModel = new RecModel(rec.cronString);
		GWT.log("trying to display: '" + rec.cronString + "'");
		try {
			showRecModel();
		} catch (Throwable t) {
			// we failed, default every one hour
			GWT.log(t.getMessage(), t);
			periodicMin.setValue(true);
			perMinFreq.setSelectedIndex(0);
		}
	}

	@UiFactory
	RadioGroup getGroup() {
		return group;
	}

	private void showRecModel() throws Exception {
		String min = recModel.getMinutes();
		String hours = recModel.getHours();
		String day = recModel.getDaysOfWeek();
		if (min.contains("*/") || min.equals("*")) {
			GWT.log("Looks like periodic min");
			periodicMin.setValue(true);
			ValueChangeEvent.<Boolean> fire(periodicMin, true);
			int freq = 1;
			int idx = min.indexOf('/');
			if (idx > 0) {
				freq = Integer.parseInt(min.substring(idx + 1));
			}

			switch (freq) {
			case 30:
				perMinFreq.setSelectedIndex(6);
				break;
			case 20:
				perMinFreq.setSelectedIndex(5);
				break;
			case 15:
				perMinFreq.setSelectedIndex(4);
				break;
			case 10:
				perMinFreq.setSelectedIndex(3);
				break;
			case 5:
				perMinFreq.setSelectedIndex(2);
				break;
			case 2:
				perMinFreq.setSelectedIndex(1);
				break;
			default:
			case 1:
				perMinFreq.setSelectedIndex(0);
			}
		} else if ("0".equals(min) && (hours.contains("*/") || hours.equals("*"))) {
			GWT.log("Looks like periodic hours");
			periodicHour.setValue(true);
			ValueChangeEvent.<Boolean> fire(periodicHour, true);
			int freq = 1;
			int idx = hours.indexOf('/');
			if (idx > 0) {
				freq = Integer.parseInt(hours.substring(idx + 1));
			}
			GWT.log("freq: " + freq);

			switch (freq) {
			case 12:
				perHourFreq.setSelectedIndex(6);
				break;
			case 8:
				perHourFreq.setSelectedIndex(5);
				break;
			case 6:
				perHourFreq.setSelectedIndex(4);
				break;
			case 4:
				perHourFreq.setSelectedIndex(3);
				break;
			case 3:
				perHourFreq.setSelectedIndex(2);
				break;
			case 2:
				perHourFreq.setSelectedIndex(1);
				break;
			default:
			case 1:
				perHourFreq.setSelectedIndex(0);
			}
		} else if (day.length() >= 2) {
			GWT.log("Looks like daily");
			daily.setValue(true);
			ValueChangeEvent.<Boolean> fire(daily, true);

			dayMon.setValue(day.contains("MON"));
			dayTue.setValue(day.contains("TUE"));
			dayWed.setValue(day.contains("WED"));
			dayThu.setValue(day.contains("THU"));
			dayFri.setValue(day.contains("FRI"));
			daySat.setValue(day.contains("SAT"));
			daySun.setValue(day.contains("SUN"));

			dayMinutes.setText(min);
			dayHour.setText(recModel.getHours());
		} else {
			GWT.log("Unhandled cron string: " + recModel.getCronString());
			throw new Exception();
		}
	}

	public void destroy() {
	}

}
