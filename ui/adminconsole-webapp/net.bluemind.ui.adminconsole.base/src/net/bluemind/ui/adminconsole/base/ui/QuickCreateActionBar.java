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
package net.bluemind.ui.adminconsole.base.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;

import net.bluemind.ui.common.client.forms.ButtonBar;

/**
 * Save / Cancel buttons on edit screens
 * 
 * 
 */
public class QuickCreateActionBar extends ButtonBar {

	public interface CrudConstants extends Constants {

		String create();

		String createEdit();

		String cancel();

	}

	private static final CrudConstants cc = GWT.create(CrudConstants.class);

	private HorizontalPanel hp;

	public QuickCreateActionBar() {
		super();
		this.hp = new HorizontalPanel();
		initWidget(hp);
	}

	public void setCancelAction(final ScheduledCommand cancel) {
		Button cb = newStdButton(cc.cancel());
		cb.getElement().setId("btn-cancel");
		cb.setTitle("Echap");
		cb.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Scheduler.get().scheduleDeferred(cancel);
			}
		});
		hp.add(cb);
	}

	public void setCreateAction(final ScheduledCommand sc) {
		final Button sb = newPrimaryButton(cc.create());
		sb.getElement().setId("btn-enter");
		sb.setTitle("Enter");
		sb.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				sb.setEnabled(false);
				Scheduler.get().scheduleDeferred(sc);
				Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

					@Override
					public boolean execute() {
						sb.setEnabled(true);
						return false;
					}
				}, 2000);
			}
		});
		hp.add(sb);
	}

	public void setCreateAndEditAction(final ScheduledCommand sc) {
		final Button sb = newPrimaryButton(cc.createEdit());
		sb.getElement().setId("btn-ctrl-enter");
		sb.setTitle("Ctrl+Enter");
		sb.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				sb.setEnabled(false);
				Scheduler.get().scheduleDeferred(sc);
				Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {

					@Override
					public boolean execute() {
						sb.setEnabled(true);
						return false;
					}
				}, 2000);
			}
		});
		hp.add(sb);
	}
}
