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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;

import net.bluemind.scheduledjob.api.LogLevel;

public class SeverityFilter extends Composite {

	private List<ISeverityFilterListener> listeners;
	private FlowPanel fp;
	int activeFilters;
	private HashSet<LogLevel> accepted;
	private HandlerRegistration successReg;
	private HandlerRegistration warnReg;
	private HandlerRegistration failureReg;
	private TextBox filter;

	public SeverityFilter() {
		listeners = new LinkedList<ISeverityFilterListener>();
		this.fp = new FlowPanel();
		this.activeFilters = 0;
		this.accepted = new HashSet<LogLevel>();
		acceptAll();

		ToggleButton tb = null;

		tb = new ToggleButton();
		tb.setStyleName("fa fa-check");
		successReg = tb.addClickHandler(createHandler(LogLevel.INFO, tb));
		tb.addStyleName("button");
		tb.addStyleName("button-first");
		tb.addStyleName("button-up");
		fp.add(tb);

		tb = new ToggleButton();
		tb.setStyleName("fa fa-warning");
		warnReg = tb.addClickHandler(createHandler(LogLevel.WARNING, tb));
		tb.addStyleName("button");
		tb.addStyleName("button-middle");
		tb.addStyleName("button-up");
		fp.add(tb);

		tb = new ToggleButton();
		tb.setStyleName("fa fa-close");
		failureReg = tb.addClickHandler(createHandler(LogLevel.ERROR, tb));
		tb.addStyleName("button");
		tb.addStyleName("button-middle");
		tb.addStyleName("button-up");
		fp.add(tb);

		this.filter = new TextBox();
		filter.addStyleName("button-last");
		fp.add(filter);
		filter.getElement().setPropertyString("placeholder", JobTexts.INST.filterPlaceholder());
		filter.addKeyDownHandler(new KeyDownHandler() {

			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					emitStatusChangedEvent();
				}
			}
		});

		initWidget(fp);
	}

	private ClickHandler createHandler(final LogLevel s, final ToggleButton tb) {
		ClickHandler ch = new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				boolean down = tb.isDown();
				if (down) {
					tb.removeStyleName("button-up");
					tb.addStyleName("button-down");
					if (activeFilters == 0) {
						accepted.clear();
					}
					accepted.add(s);
					activeFilters++;
				} else {
					tb.removeStyleName("button-down");
					tb.addStyleName("button-up");
					accepted.remove(s);
					activeFilters--;
					if (activeFilters == 0) {
						acceptAll();
					}
				}
				emitStatusChangedEvent();
			}
		};
		return ch;
	}

	private void acceptAll() {
		accepted.add(LogLevel.PROGRESS);
		accepted.add(LogLevel.INFO);
		accepted.add(LogLevel.WARNING);
		accepted.add(LogLevel.ERROR);
	}

	public Set<LogLevel> getAcceptedStatus() {
		return accepted;
	}

	public void addListener(ISeverityFilterListener sfl) {
		listeners.add(sfl);
	}

	public void removeListener(ISeverityFilterListener sfl) {
		listeners.remove(sfl);
	}

	private void emitStatusChangedEvent() {
		for (ISeverityFilterListener sfl : listeners) {
			sfl.filteredStatusChanged(getAcceptedStatus(), filter.getText());
		}
	}

	public void destroy() {
		listeners.clear();
		successReg.removeHandler();
		warnReg.removeHandler();
		failureReg.removeHandler();
	}

}
