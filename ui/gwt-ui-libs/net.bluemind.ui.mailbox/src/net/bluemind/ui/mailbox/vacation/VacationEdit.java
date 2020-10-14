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
import com.google.gwt.user.datepicker.client.CalendarUtil;
import com.google.gwt.user.datepicker.client.DateBox;

import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.MailFilter.Vacation;

public class VacationEdit extends Composite {

	private static final VacationConstants constants = GWT.create(VacationConstants.class);

	private RadioButton forbid;
	private CheckBox allow;
	private CheckBox cbTo;
	private DateBox from;
	private DateBox to;
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

		to = new DateBox();
		to.getElement().setId("vacation-date-to");
		to.setFormat(new DateBox.DefaultFormat(dateFormat));

		cbTo = new CheckBox(constants.vacationTo());
		cbTo.getElement().setId("vacation-to");
		cbTo.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				if (cbTo.getValue()) {
					to.setEnabled(true);
				} else {
					to.setEnabled(false);
					to.setValue(null);
				}

			}
		});

		HorizontalPanel dates = new HorizontalPanel();
		dates.add(from);
		dates.add(cbTo);
		dates.add(to);

		message = new TextArea();
		message.getElement().setId("vacation-text");
		message.setHeight("100px");
		message.setWidth("400px");

		subject = new TextBox();
		subject.getElement().setId("vacation-subject");

		int i = 0;
		content.setWidget(i, 0, new Label(constants.vacationFrom()));
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

	public void setValue(Vacation vs) {
		subject.setText(vs.subject);
		message.setText(vs.text);
		if (vs.enabled) {
			allow();
			from.setValue(vs.start);
			if (vs.end != null) {
				cbTo.setEnabled(true);
				cbTo.setValue(true);
				to.setEnabled(true);

				Date dateEnd = vs.end;
				CalendarUtil.addDaysToDate(dateEnd, -1);
				to.setValue(dateEnd);
			} else {
				cbTo.setEnabled(true);
				cbTo.setValue(false);
				to.setEnabled(false);
			}
		} else {
			forbid();
		}
	}

	public MailFilter.Vacation getValue() {
		MailFilter.Vacation vs = new MailFilter.Vacation();
		vs.enabled = allow.getValue();
		vs.subject = subject.getText();
		vs.text = message.getText();
		if (vs.enabled) {
			vs.start = from.getValue();

			Date dateEnd = to.getValue();
			if (dateEnd != null) {
				CalendarUtil.addDaysToDate(dateEnd, 1);
			}
			vs.end = dateEnd;
		}
		return vs;
	}

	private void forbid() {
		forbid.setValue(true);
		from.setEnabled(false);
		from.setValue(null);
		cbTo.setEnabled(false);
		cbTo.setValue(false);
		to.setEnabled(false);
		to.setValue(null);
		subject.setEnabled(false);
		message.setEnabled(false);
	}

	private void allow() {
		allow.setValue(true);
		from.setEnabled(true);
		cbTo.setEnabled(true);
		cbTo.setValue(false);
		to.setEnabled(false);
		to.setValue(null);
		subject.setEnabled(true);
		message.setEnabled(true);
	}

}
