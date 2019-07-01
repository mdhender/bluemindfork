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
package net.bluemind.ui.adminconsole.filehosting.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.gwt.endpoint.FileHostingGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.systemconf.util.ValueUtil;

public class EditFileHostingSettingsEditor extends CompositeGwtWidgetElement {
	static final String TYPE = "bm.ac.EditDomainFilehostingEditor";

	private static final int DEFAULT_AUTO_DETACHMENT_LIMIT = 10;
	private static final int DEFAULT_MAX_DETACHMENT_SIZE_LIMIT = 100;
	private static final int DEFAULT_RETENTION = 365;

	private static EditDomainFileHostingUiBinder uiBinder = GWT.create(EditDomainFileHostingUiBinder.class);

	interface EditDomainFileHostingUiBinder extends UiBinder<HTMLPanel, EditFileHostingSettingsEditor> {
	}

	@UiField
	IntegerBox retentionTime;

	@UiField
	IntegerBox detachedAttachmentSizeLimit;

	@UiField
	IntegerBox autoDetachmentLimit;

	@UiField
	CheckBox backupFileHostingData;

	@UiField
	Label noImplWarning;

	@UiField
	Label backupFileHostingDataLabel;

	private String skipTagList;

	private boolean domainConfig;

	protected EditFileHostingSettingsEditor() {
		this(false);
	}

	protected EditFileHostingSettingsEditor(boolean domainConfig) {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		this.domainConfig = domainConfig;
		initWidget(panel);

	}

	@Override
	public void loadModel(JavaScriptObject model) {
		noImplWarning.setVisible(false);
		SettingsModel map = settingsModel(model);
		if (map.get(GlobalSettingsKeys.filehosting_retention.name()) != null) {
			retentionTime.setText(map.get(GlobalSettingsKeys.filehosting_retention.name()).toString());
		} else {
			retentionTime.setText("" + DEFAULT_RETENTION);
		}
		detachedAttachmentSizeLimit.setText(readMbIntegerValue(map, GlobalSettingsKeys.filehosting_max_filesize,
				DEFAULT_MAX_DETACHMENT_SIZE_LIMIT));
		autoDetachmentLimit.setText(
				readMbIntegerValue(map, GlobalSettingsKeys.mail_autoDetachmentLimit, DEFAULT_AUTO_DETACHMENT_LIMIT));

		FileHostingGwtEndpoint service = new FileHostingGwtEndpoint(Ajax.TOKEN.getSessionId(),
				Ajax.TOKEN.getContainerUid());
		service.info(new DefaultAsyncHandler<FileHostingInfo>() {

			@Override
			public void success(FileHostingInfo value) {
				noImplWarning.setVisible(!value.present);
			}
		});

		if (domainConfig) {
			backupFileHostingData.setVisible(false);
			backupFileHostingDataLabel.setVisible(false);
		} else {
			backupFileHostingData.setValue(true);
			backupFileHostingDataLabel.setVisible(true);
			if (map.get(SysConfKeys.dpBackupSkipTags.name()) != null) {
				this.skipTagList = map.get(SysConfKeys.dpBackupSkipTags.name()).toString();
				if (this.skipTagList.contains("filehosting/data")) {
					backupFileHostingData.setValue(false);
				}
			} else {
				this.skipTagList = "";
			}
		}

	}

	protected SettingsModel settingsModel(JavaScriptObject model) {
		return SettingsModel.globalSettingsFrom(model);
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		String sanitizedMaxDetachmentSize = sanitizeMbIntegerValue(detachedAttachmentSizeLimit,
				DEFAULT_MAX_DETACHMENT_SIZE_LIMIT);
		String sanitizedAutoDetachmentSize = sanitizeMbIntegerValue(autoDetachmentLimit, DEFAULT_AUTO_DETACHMENT_LIMIT);

		SettingsModel map = settingsModel(model);

		map.putString(GlobalSettingsKeys.filehosting_retention.name(), sanitizeDayIntegerValue(retentionTime, 365));
		map.putString(GlobalSettingsKeys.filehosting_max_filesize.name(), sanitizedMaxDetachmentSize);
		map.putString(GlobalSettingsKeys.mail_autoDetachmentLimit.name(), sanitizedAutoDetachmentSize);
		if (!domainConfig) {
			if (backupFileHostingData.getValue()) {
				if (this.skipTagList.contains("filehosting/data")) {
					removeTag("filehosting/data");
				}
			} else {
				if (!this.skipTagList.contains("filehosting/data")) {
					addTag("filehosting/data");
				}
			}
			map.putString(SysConfKeys.dpBackupSkipTags.name(), this.skipTagList);
		}
	}

	private void addTag(String tag) {
		List<String> stringList = stringList(this.skipTagList);
		stringList.add(tag);
		updateTagList(stringList);
	}

	private void removeTag(String tag) {
		List<String> stringList = stringList(this.skipTagList);
		stringList.remove(tag);
		updateTagList(stringList);
	}

	private void updateTagList(List<String> stringList) {
		StringBuilder sb = new StringBuilder();
		for (String string : stringList) {
			sb.append(string + ",");
		}
		sb.deleteCharAt(sb.length() - 1);
		this.skipTagList = sb.toString();
	}

	public List<String> stringList(String value) {
		if (value == null || value.trim().length() == 0) {
			return new ArrayList<>();
		} else {
			return new ArrayList<>(Arrays.asList(value.split(",")));
		}
	}

	private String sanitizeDayIntegerValue(IntegerBox field, int defaultValue) {
		return ValueUtil.removeNonDigitCharacters(field.getText(), defaultValue);
	}

	private String readMbIntegerValue(SettingsModel map, GlobalSettingsKeys key, int defaultValue) {
		String limit = null;
		if (map.get(key.name()) == null) {
			limit = "" + defaultValue;
		} else {
			limit = map.get(key.name()).toString();
		}
		return toMbyte(defaultValue, limit);
	}

	private String toMbyte(int defaultValue, String limit) {
		limit = ValueUtil.removeNonDigitCharacters(limit, defaultValue);
		long limitLong = Long.parseLong(limit) / (1024 * 1024);
		return String.valueOf(limitLong);
	}

	private String sanitizeMbIntegerValue(IntegerBox field, int defaultValue) {
		try {
			float asFloat= Math.round(Float.parseFloat(field.getText()));
			long value = ((long)asFloat) * (1024 * 1024);	
			return String.valueOf(value);
		} catch (Exception e){
			return String.valueOf(defaultValue);
		}
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditFileHostingSettingsEditor();
			}
		});
	}

}
