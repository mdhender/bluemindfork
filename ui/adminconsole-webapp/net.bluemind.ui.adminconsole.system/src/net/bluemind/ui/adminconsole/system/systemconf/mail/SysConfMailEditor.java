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
package net.bluemind.ui.adminconsole.system.systemconf.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.adminconsole.system.systemconf.SysConfModel;
import net.bluemind.ui.adminconsole.system.systemconf.util.ValueUtil;

public class SysConfMailEditor extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.ac.SysConfMailEditor";
	private static final int DEFAULT_MESSAGE_SIZE_LIMIT = 10;

	@UiField
	TextBox myNetworks;

	@UiField
	IntegerBox maxMailSize;

	@UiField
	TextBox relayHost;

	@UiField
	IntegerBox cyrusMaxChild;

	@UiField
	IntegerBox cyrusRetentionTime;

	@UiField
	CheckBox archiveEnabled;

	@UiField
	IntegerBox archiveDays;

	@UiField
	IntegerBox archiveSizeThreshold;

	private static SysConfMailUiBinder uiBinder = GWT.create(SysConfMailUiBinder.class);

	interface SysConfMailUiBinder extends UiBinder<HTMLPanel, SysConfMailEditor> {
	}

	protected SysConfMailEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new SysConfMailEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);

		myNetworks.setText(map.get(SysConfKeys.mynetworks.name()).toString());
		maxMailSize.setText(readMbIntegerValue(map, SysConfKeys.message_size_limit, DEFAULT_MESSAGE_SIZE_LIMIT));
		if (map.get(SysConfKeys.relayhost.name()) != null) {
			relayHost.setText(map.get(SysConfKeys.relayhost.name()).toString());
		}

		if (map.get(SysConfKeys.imap_max_child.name()) != null) {
			cyrusMaxChild.setText(map.get(SysConfKeys.imap_max_child.name()));
		} else {
			cyrusMaxChild.setText("");
		}

		if (map.get(SysConfKeys.cyrus_expunged_retention_time.name()) != null) {
			cyrusRetentionTime.setText(map.get(SysConfKeys.cyrus_expunged_retention_time.name()));
		} else {
			cyrusRetentionTime.setText("");
		}

		if (map.get(SysConfKeys.archive_enabled.name()) != null) {
			archiveEnabled.setValue(Boolean.parseBoolean(map.get(SysConfKeys.archive_enabled.name())));
			if (archiveEnabled.getValue()) {
				archiveDays.setText(map.get(SysConfKeys.archive_days.name()));
				archiveSizeThreshold.setText(readArchiveSizeThreshold(map, SysConfKeys.archive_size_threshold, 1));
			}
		} else {
			archiveEnabled.setValue(false);
		}

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		String sanitizedMaxMessageSize = sanitizeMbIntegerValue(maxMailSize, DEFAULT_MESSAGE_SIZE_LIMIT);
		SysConfModel map = SysConfModel.from(model);

		map.putString(SysConfKeys.mynetworks.name(), myNetworks.getText());
		map.putString(SysConfKeys.message_size_limit.name(), sanitizedMaxMessageSize);
		map.putString(SysConfKeys.relayhost.name(), relayHost.getText());

		maxMailSize.setText("" + Integer.parseInt(sanitizedMaxMessageSize) / (1024 * 1024));

		map.putString(SysConfKeys.archive_enabled.name(), archiveEnabled.getValue() ? "true" : "false");

		if (archiveDays.getValue() != null) {
			map.putString(SysConfKeys.archive_days.name(), "" + archiveDays.getValue().intValue());
		} else {
			map.putString(SysConfKeys.archive_days.name(), "7");
		}

		map.putString(SysConfKeys.archive_size_threshold.name(), sanitizeArchiveSizeThreshold(archiveSizeThreshold, 1));

		map.putString(SysConfKeys.imap_max_child.name(), cyrusMaxChild.getText());

		if (cyrusRetentionTime.getValue() != null) {
			map.putString(SysConfKeys.cyrus_expunged_retention_time.name(), cyrusRetentionTime.getText());
		}
	}

	private String readArchiveSizeThreshold(SysConfModel map, SysConfKeys key, int defaultValue) {
		String limit = map.get(key.name()).toString();
		limit = ValueUtil.removeNonDigitCharacters(limit, defaultValue);
		int limitInt = Integer.parseInt(limit) / 1024;
		return String.valueOf(limitInt);
	}

	private String sanitizeArchiveSizeThreshold(IntegerBox archiveSizeThreshold, int defaultValue) {
		String limit = ValueUtil.removeNonDigitCharacters(archiveSizeThreshold.getText(), defaultValue);
		int intLimit = Integer.parseInt(limit);
		intLimit = intLimit * 1024;
		return String.valueOf(intLimit);
	}

	private String readMbIntegerValue(SysConfModel map, SysConfKeys key, int defaultValue) {
		String limit = map.get(key.name()).toString();
		limit = ValueUtil.removeNonDigitCharacters(limit, defaultValue);
		int limitInt = Integer.parseInt(limit) / (1024 * 1024);
		return String.valueOf(limitInt);
	}

	private String sanitizeMbIntegerValue(IntegerBox field, int defaultValue) {
		String limit = ValueUtil.removeNonDigitCharacters(field.getText(), defaultValue);
		int intLimit = Integer.parseInt(limit);
		intLimit = intLimit * (1024 * 1024);
		return String.valueOf(intLimit);
	}

}
