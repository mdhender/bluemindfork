/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.ui.adminconsole.system.domains.edit.extcal;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public class EditExternalCalendarsEditor extends CompositeGwtWidgetElement {
	private static final int HOUR_MILLIS = 3600000;

	public static final String TYPE = "bm.ac.EditExternalCalendarsEditor";

	private static EditExternalCalendarsEditorUiBinder uiBinder = GWT.create(EditExternalCalendarsEditorUiBinder.class);

	interface EditExternalCalendarsEditorUiBinder extends UiBinder<HTMLPanel, EditExternalCalendarsEditor> {
	}

	@UiField
	ListBox minDelayBox;

	@UiField
	Label minDelayWarningIcon;

	@UiField
	Label minDelayWarning;

	@UiField
	Label minutes;

	@UiField
	IntegerBox minDelayOther;

	protected EditExternalCalendarsEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		minDelayWarningIcon.setStyleName("fa fa-lg fa-warning red");
		minDelayBox.addChangeHandler(e -> {
			if (minDelayBox.getSelectedIndex() == MinDelay.DELAY_OTHER.ordinal()) {
				this.setMinutesVisibility(true);
			} else {
				this.setMinutesVisibility(false);
			}
		});

		for (final MinDelay minDelay : MinDelay.values()) {
			this.minDelayBox.addItem(minDelay.text(), String.valueOf(minDelay.getDelay()));
		}

		minDelayOther.addKeyPressHandler(e -> {
			if (!Character.isDigit(e.getCharCode())) {
				((IntegerBox) e.getSource()).cancelKey();
			}
		});

		final Style iconStyle = minDelayWarningIcon.getElement().getStyle();
		iconStyle.setProperty("marginLeft", "5px");
		iconStyle.setProperty("marginRight", "5px");

		initWidget(panel);
	}

	private void setMinutesVisibility(final boolean visible) {
		minDelayWarningIcon.setVisible(visible);
		minDelayWarning.setVisible(visible);
		minutes.setVisible(visible);
		minDelayOther.setVisible(visible);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditExternalCalendarsEditor();
			}
		});
	}

	private static enum MinDelay {
		DELAY_24_HOURS(24 * HOUR_MILLIS), DELAY_6_HOURS(6 * HOUR_MILLIS), DELAY_3_HOURS(3 * HOUR_MILLIS), DELAY_1_HOUR(
				HOUR_MILLIS), DELAY_OTHER(-1);
		private long delay;

		private MinDelay(final long delay) {
			this.delay = delay;
		}

		public long getDelay() {
			return delay;
		}

		public String text() {
			switch (this) {
			case DELAY_OTHER:
				return DomainConstants.INST.other();
			case DELAY_1_HOUR:
				return "1 " + DomainConstants.INST.hour();
			default:
				return this.delay / HOUR_MILLIS + " " + DomainConstants.INST.hours();
			}
		}

		public static MinDelay from(final long minDelay) {
			if (minDelay == 24 * HOUR_MILLIS) {
				return DELAY_24_HOURS;
			} else if (minDelay == 6 * HOUR_MILLIS) {
				return DELAY_6_HOURS;
			} else if (minDelay == 3 * HOUR_MILLIS) {
				return DELAY_3_HOURS;
			} else if (minDelay == HOUR_MILLIS) {
				return DELAY_1_HOUR;
			} else {
				return DELAY_OTHER;
			}
		}
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		final String minDelayString = SettingsModel.domainSettingsFrom(model)
				.get("domain.setting.calendar.sync.min.delay");
		final long minDelayValue = minDelayString != null ? Long.valueOf(minDelayString)
				: MinDelay.DELAY_24_HOURS.getDelay();
		final MinDelay minDelay = MinDelay.from(minDelayValue);
		this.minDelayBox.setItemSelected(minDelay.ordinal(), true);
		if (minDelay == MinDelay.DELAY_OTHER) {
			final int minutes = (int) (minDelayValue / 1000 / 60);
			this.minDelayOther.setValue(minutes);
			this.setMinutesVisibility(true);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		String minDelay = this.minDelayBox.getSelectedValue();
		if (minDelay.equals("-1")) {
			minDelay = String.valueOf(this.minDelayOther.getValue() * 60 * 1000);
		}
		SettingsModel.domainSettingsFrom(model).putString("domain.setting.calendar.sync.min.delay", minDelay);
	}

}
