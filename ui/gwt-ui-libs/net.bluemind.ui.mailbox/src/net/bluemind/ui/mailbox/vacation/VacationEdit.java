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
package net.bluemind.ui.mailbox.vacation;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.datepicker.client.DateBox;

import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Vacation;
import net.bluemind.ui.common.client.forms.TimePickerMs;

public class VacationEdit extends Composite {

	private static final VacationConstants constants = GWT.create(VacationConstants.class);

	private RadioButton forbid;
	private CheckBox allow;
	private CheckBox cbFrom;
	private CheckBox cbTo;
	private DateBox from;
	private TimePickerMs fromTime;
	private DateBox to;
	private TimePickerMs toTime;
	private TextBox subject;
	private TextArea message;

	public VacationEdit() {
		FlexTable container = new FlexTable();

		forbid = new RadioButton("vacation", constants.vacationForbid());
		forbid.getElement().setId("vacation-forbid");
		forbid.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				forbid();
			}
		});

		allow = new RadioButton("vacation", constants.vacationAllow());
		allow.getElement().setId("vacation-allow");
		allow.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				allow();
			}
		});

		FlexTable content = new FlexTable();

		DateTimeFormat dateFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_LONG);

		from = new DateBox();
		from.getElement().setId("vacation-date-from");
		from.setFormat(new DateBox.DefaultFormat(dateFormat));

		fromTime = new TimePickerMs();
		fromTime.getElement().setId("vacation-date-from-time");

		cbFrom = initDateCheckbox(constants.vacationFrom(), "vacation-from", from, fromTime);

		to = new DateBox();
		to.getElement().setId("vacation-date-to");
		to.setFormat(new DateBox.DefaultFormat(dateFormat));

		toTime = new TimePickerMs();
		toTime.getElement().setId("vacation-date-to-time");

		cbTo = initDateCheckbox(constants.vacationTo(), "vacation-to", to, toTime);

		HorizontalPanel dateFrom = new HorizontalPanel();
		dateFrom.add(cbFrom);
		dateFrom.add(from);
		dateFrom.add(fromTime);

		HorizontalPanel dateTo = new HorizontalPanel();
		dateTo.getElement().getStyle().setMarginLeft(10, Unit.PX);
		dateTo.add(cbTo);
		dateTo.add(to);
		dateTo.add(toTime);

		HorizontalPanel dates = new HorizontalPanel();
		dates.add(dateFrom);
		dates.add(dateTo);

		message = new TextArea();
		message.getElement().setId("vacation-text");
		message.setHeight("100px");
		message.setWidth("400px");

		subject = new TextBox();
		subject.getElement().setId("vacation-subject");

		int i = 0;
		content.setWidget(i, 1, dates);

		i++;
		content.setWidget(i, 0, new Label(constants.subject()));
		content.setWidget(i, 1, subject);

		i++;
		content.setWidget(i, 0, new Label(constants.message()));
		content.setWidget(i, 1, message);

		i = 0;
		container.setWidget(i++, 0, forbid);
		container.setWidget(i++, 0, allow);
		container.setWidget(i++, 0, content);

		initWidget(container);
	}

	private CheckBox initDateCheckbox(String label, String id, DateBox db, TimePickerMs tpm) {
		CheckBox newCb = new CheckBox(label);
		newCb.getElement().setId(id);
		newCb.getElement().getStyle().setMarginRight(3, Unit.PX);
		newCb.getElement().getFirstChildElement().getStyle().setMarginTop(4, Unit.PX);
		newCb.getElement().getFirstChildElement().getStyle().setMarginRight(3, Unit.PX);
		newCb.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				switchDateField(newCb, db, tpm);
			}
		});

		return newCb;
	}

	private void switchDateField(CheckBox cb, DateBox db, TimePickerMs tpm) {
		if (cb.getValue()) {
			db.setEnabled(true);
			tpm.setEnabled(true);
		} else {
			db.setEnabled(false);
			db.setValue(null);
			tpm.setEnabled(false);
			tpm.setValue(0);
		}
	}

	public void setValue(Vacation vs) {
		subject.setText(vs.subject);
		message.setText(vs.text);
		if (vs.enabled) {
			allow();

			setDate(vs.start, cbFrom, from, fromTime);
			setDate(vs.end, cbTo, to, toTime);
		} else {
			forbid();
		}
	}

	private void setDate(Date date, CheckBox cb, DateBox db, TimePickerMs tpm) {
		if (date != null) {
			cb.setEnabled(true);
			cb.setValue(true);
			db.setEnabled(true);
			tpm.setEnabled(true);

			Date dateEnd = date;
			db.setValue(dateEnd);
			tpm.setValue(getTimeMs(dateEnd));

			return;
		}

		cb.setEnabled(true);
		cb.setValue(false);
		db.setEnabled(false);
		tpm.setEnabled(false);
	}

	private int getTimeMs(Date date) {
		return date.getHours() * 60 * 60 * 1000 + date.getMinutes() * 60 * 1000;
	}

	public MailFilter.Vacation getValue() {
		MailFilter.Vacation vs = new MailFilter.Vacation();
		vs.enabled = allow.getValue();
		vs.subject = subject.getText();
		vs.text = message.getText();

		if (vs.enabled) {
			vs.start = getDate(from, fromTime);
			vs.end = getDate(to, toTime);
		}
		return vs;
	}

	private Date getDate(DateBox db, TimePickerMs tpm) {
		Date dateEnd = db.getValue();
		if (dateEnd != null) {
			return new Date(dateEnd.getTime() + Integer.parseInt(tpm.getSelectedValue()));
		}

		return null;
	}

	private void forbid() {
		forbid.setValue(true);

		cbFrom.setEnabled(false);
		cbFrom.setValue(false);
		from.setEnabled(false);
		from.setValue(null);
		fromTime.setEnabled(false);

		cbTo.setEnabled(false);
		cbTo.setValue(false);
		to.setEnabled(false);
		to.setValue(null);
		toTime.setEnabled(false);

		subject.setEnabled(false);
		message.setEnabled(false);
	}

	private void allow() {
		allow.setValue(true);

		cbFrom.setEnabled(true);
		cbFrom.setValue(false);
		from.setEnabled(from.getValue() != null);
		fromTime.setEnabled(from.getValue() != null);

		cbTo.setEnabled(true);
		cbTo.setValue(false);
		to.setEnabled(false);
		to.setValue(null);
		toTime.setEnabled(false);

		subject.setEnabled(true);
		message.setEnabled(true);
	}
}
