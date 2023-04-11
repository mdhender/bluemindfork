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

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.ui.adminconsole.system.systemconf.SysConfModel;
import net.bluemind.ui.adminconsole.system.systemconf.mail.l10n.SysConfMailConstants;
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
	IntegerBox retentionTimeTextBox;

	@UiField
	ListBox archiveKindSelectBox;

	@UiField
	IntegerBox archiveDays;

	@UiField
	IntegerBox archiveSizeThreshold;

	@UiField
	TableElement archiveCyrusTable;

	@UiField
	TableElement archiveS3Table;

	@UiField
	TableElement archiveScalityTable;

	@UiField
	TextBox s3EndpointAddress;

	@UiField
	TextBox s3Region;

	@UiField
	TextBox s3AccessKey;

	@UiField
	PasswordTextBox s3SecretKey;

	@UiField
	TextBox s3BucketName;

	@UiField
	IntegerBox s3SdsBackupRetentionDays;

	@UiField
	IntegerBox scalitySdsBackupRetentionDays;

	@UiField
	TextBox scalityEndpointAddress;

	private static SysConfMailUiBinder uiBinder = GWT.create(SysConfMailUiBinder.class);

	private enum ArchiveKindValue {
		none(SysConfMailConstants.INST.archiveKindNone(), "none", 0),
		cyrus(SysConfMailConstants.INST.archiveKindCyrus(), "cyrus", 1),
		s3(SysConfMailConstants.INST.archiveKindS3(), "s3", 2),
		scalityring(SysConfMailConstants.INST.archiveKindScalityRing(), "scalityring", 3);

		private String display;
		private String value;
		private int index;

		private ArchiveKindValue(String display, String value, int index) {
			this.display = display;
			this.value = value;
			this.index = index;
		}
	}

	interface SysConfMailUiBinder extends UiBinder<HTMLPanel, SysConfMailEditor> {
	}

	protected SysConfMailEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);

		for (ArchiveKindValue val : ArchiveKindValue.values()) {
			archiveKindSelectBox.addItem(val.display, val.value);
		}

		archiveKindSelectBox.addChangeHandler(event -> {
			updateArchiveTablesVisibilities();
		});
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, (widgetelement) -> {
			return new SysConfMailEditor();
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		final String _CYRUS_RETENTION_TIME_DEFAULT = "7";
		final String _CYRUS_ARCHIVE_DAYS_DEFAULT = "7";
		final String _ARCHIVE_KIND_DEFAULT = ArchiveKindValue.none.name();
		final String _SDS_BACKUP_RETENTION_DAYS_DEFAULT = "90";

		SysConfModel map = SysConfModel.from(model);
		myNetworks.setText(map.get(SysConfKeys.mynetworks.name()));
		maxMailSize.setText(readMbIntegerValue(map, SysConfKeys.message_size_limit, DEFAULT_MESSAGE_SIZE_LIMIT));
		if (map.get(SysConfKeys.relayhost.name()) != null) {
			relayHost.setText(map.get(SysConfKeys.relayhost.name()));
		}

		String retentionTime = Optional.ofNullable(map.get(SysConfKeys.cyrus_expunged_retention_time.name()))
				.orElse(_CYRUS_RETENTION_TIME_DEFAULT);
		retentionTimeTextBox.setText(retentionTime);

		int archiveKindIndex = ArchiveKindValue.valueOf(
				Optional.ofNullable(map.get(SysConfKeys.archive_kind.name())).orElse(_ARCHIVE_KIND_DEFAULT)).index;
		archiveKindSelectBox.setSelectedIndex(archiveKindIndex);

		archiveDays.setText(
				Optional.ofNullable(map.get(SysConfKeys.archive_days.name())).orElse(_CYRUS_ARCHIVE_DAYS_DEFAULT));
		archiveSizeThreshold.setText(readArchiveSizeThreshold(map, SysConfKeys.archive_size_threshold, 1024));

		s3SdsBackupRetentionDays.setTitle(SysConfMailConstants.INST.sdsBackupRetentionDaysTooltip());
		scalitySdsBackupRetentionDays.setTitle(SysConfMailConstants.INST.sdsBackupRetentionDaysTooltip());

		switch (archiveKindIndex) {
		case 0:
		case 1:
			archiveKindSelectBox.removeItem(3);
			archiveKindSelectBox.removeItem(2);
			break;
		case 2:
			s3EndpointAddress.setText(map.get(SysConfKeys.sds_s3_endpoint.name()));
			s3Region.setText(map.get(SysConfKeys.sds_s3_region.name()));
			s3AccessKey.setText(map.get(SysConfKeys.sds_s3_access_key.name()));
			s3SecretKey.setText(map.get(SysConfKeys.sds_s3_secret_key.name()));
			s3BucketName.setText(map.get(SysConfKeys.sds_s3_bucket.name()));
			s3SdsBackupRetentionDays.setText(Optional.ofNullable(map.get(SysConfKeys.sds_backup_rentention_days.name()))
					.orElse(_SDS_BACKUP_RETENTION_DAYS_DEFAULT));
			break;
		case 3:
			scalityEndpointAddress.setText(map.get(SysConfKeys.sds_s3_endpoint.name()));
			scalitySdsBackupRetentionDays
					.setText(Optional.ofNullable(map.get(SysConfKeys.sds_backup_rentention_days.name()))
							.orElse(_SDS_BACKUP_RETENTION_DAYS_DEFAULT));
			break;
		default:
			break;
		}

		updateArchiveTablesVisibilities();
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		SysConfModel map = SysConfModel.from(model);
		map.putString(SysConfKeys.mynetworks.name(), myNetworks.getText());
		String sanitizedMaxMessageSize = sanitizeMbIntegerValue(maxMailSize, DEFAULT_MESSAGE_SIZE_LIMIT);
		map.putString(SysConfKeys.message_size_limit.name(), sanitizedMaxMessageSize);
		map.putString(SysConfKeys.relayhost.name(), relayHost.getText());

		maxMailSize.setText("" + Integer.parseInt(sanitizedMaxMessageSize) / (1024 * 1024));

		map.putString(SysConfKeys.archive_kind.name(), archiveKindSelectBox.getSelectedValue());

		map.putString(SysConfKeys.archive_days.name(),
				Optional.ofNullable("" + archiveDays.getValue().intValue()).orElse("7"));

		map.putString(SysConfKeys.archive_size_threshold.name(), sanitizeArchiveSizeThreshold(archiveSizeThreshold, 1));

		switch (archiveKindSelectBox.getSelectedIndex()) {
		case 2:
			map.putString(SysConfKeys.sds_s3_endpoint.name(), s3EndpointAddress.getText());
			map.putString(SysConfKeys.sds_s3_region.name(), s3Region.getText());
			map.putString(SysConfKeys.sds_s3_access_key.name(), s3AccessKey.getText());
			map.putString(SysConfKeys.sds_s3_secret_key.name(), s3SecretKey.getText());
			map.putString(SysConfKeys.sds_s3_bucket.name(), s3BucketName.getText());
			map.putString(SysConfKeys.sds_backup_rentention_days.name(), s3SdsBackupRetentionDays.getText());
			break;
		case 3:
			map.putString(SysConfKeys.sds_s3_endpoint.name(), scalityEndpointAddress.getText());
			map.putString(SysConfKeys.sds_backup_rentention_days.name(), scalitySdsBackupRetentionDays.getText());
			break;
		default:
			break;
		}

		if (retentionTimeTextBox.getValue() != null) {
			map.putString(SysConfKeys.cyrus_expunged_retention_time.name(), retentionTimeTextBox.getText());
		}
	}

	private String readArchiveSizeThreshold(SysConfModel map, SysConfKeys key, int defaultValue) {
		String limit = Optional.ofNullable(map.get(key.name())).orElse(Integer.toString(defaultValue));
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
		String limit = map.get(key.name());
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

	private void updateArchiveTablesVisibilities() {
		switch (archiveKindSelectBox.getSelectedValue()) {
		case "cyrus":
			archiveS3Table.setAttribute("style", "display:none");
			archiveCyrusTable.setAttribute("style", "display:block");
			archiveScalityTable.setAttribute("style", "display:none");
			break;
		case "s3":
			archiveS3Table.setAttribute("style", "display:block");
			archiveCyrusTable.setAttribute("style", "display:none");
			archiveScalityTable.setAttribute("style", "display:none");
			break;
		case "scalityring":
			archiveS3Table.setAttribute("style", "display:none");
			archiveCyrusTable.setAttribute("style", "display:none");
			archiveScalityTable.setAttribute("style", "display:block");
			break;
		case "none":
			archiveS3Table.setAttribute("style", "display:none");
			archiveCyrusTable.setAttribute("style", "display:none");
			archiveScalityTable.setAttribute("style", "display:none");
			break;
		}
	}
}
