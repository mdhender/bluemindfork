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
package net.bluemind.ui.adminconsole.system.domains.edit.mailsystem;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.ui.admin.client.forms.QuotaEdit;
import net.bluemind.ui.adminconsole.system.SettingsModel;
import net.bluemind.ui.adminconsole.system.domains.assignments.AssignmentWidget;

public class EditDomainMailsystemEditor extends AssignmentWidget {

	public static final String TYPE = "bm.ac.EditDomainMailsystemEditor";

	private static EditDomainMailsystemEditorUiBinder uiBinder = GWT.create(EditDomainMailsystemEditorUiBinder.class);

	interface EditDomainMailsystemEditorUiBinder extends UiBinder<HTMLPanel, EditDomainMailsystemEditor> {
	}

	@UiField
	ListBox internalMailServer;

	@UiField
	ListBox mailRelay;

	@UiField
	ListBox mailboxStorageServer;

	@UiField
	QuotaEdit maxUserQuota;

	@UiField
	QuotaEdit defaultUserQuota;

	@UiField
	QuotaEdit maxPublicFolderQuota;

	@UiField
	QuotaEdit defaultPublicFolderQuota;

	@UiField
	TextBox relayforSplittedDomains;

	@UiField
	CheckBox forwardUnknownEmails;

	@Override
	protected List<TagListBoxMapping> getMapping() {
		List<AssignmentWidget.TagListBoxMapping> mapping = Arrays.asList(new AssignmentWidget.TagListBoxMapping[] {
				new AssignmentWidget.TagListBoxMapping(TagDescriptor.mail_smtp.getTag(), internalMailServer),
				new AssignmentWidget.TagListBoxMapping(TagDescriptor.mail_smtp_edge.getTag(), mailRelay),
				new AssignmentWidget.TagListBoxMapping(TagDescriptor.mail_imap.getTag(), mailboxStorageServer) });
		return mapping;
	}

	protected EditDomainMailsystemEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditDomainMailsystemEditor();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
		SettingsModel map = SettingsModel.domainSettingsFrom(model);
		loadQuota(map, DomainSettingsKeys.mailbox_max_user_quota, maxUserQuota);
		loadQuota(map, DomainSettingsKeys.mailbox_default_user_quota, defaultUserQuota);
		loadQuota(map, DomainSettingsKeys.mailbox_max_publicfolder_quota, maxPublicFolderQuota);
		loadQuota(map, DomainSettingsKeys.mailbox_default_publicfolder_quota, defaultPublicFolderQuota);
		relayforSplittedDomains.setText(map.get(DomainSettingsKeys.mail_routing_relay.name()));
		String forward = map.get(DomainSettingsKeys.mail_forward_unknown_to_relay.name());
		if (null != forward && forward.equals("true")) {
			forwardUnknownEmails.setValue(true);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		super.saveModel(model);
		SettingsModel map = SettingsModel.domainSettingsFrom(model);
		saveQuota(map, DomainSettingsKeys.mailbox_max_user_quota, maxUserQuota);
		saveQuota(map, DomainSettingsKeys.mailbox_default_user_quota, defaultUserQuota);
		saveQuota(map, DomainSettingsKeys.mailbox_max_publicfolder_quota, maxPublicFolderQuota);
		saveQuota(map, DomainSettingsKeys.mailbox_default_publicfolder_quota, defaultPublicFolderQuota);
		map.putString(DomainSettingsKeys.mail_routing_relay.name(), relayforSplittedDomains.getText());
		map.putString(DomainSettingsKeys.mail_forward_unknown_to_relay.name(),
				String.valueOf(forwardUnknownEmails.getValue()));
	}

	private void loadQuota(SettingsModel map, DomainSettingsKeys key, QuotaEdit quotaEdit) {
		String prop = map.get(key.name());
		try {
			quotaEdit.setStringValue(prop);
		} catch (NumberFormatException | NullPointerException nfe) {
			quotaEdit.setStringValue("0");
		}
	}

	private void saveQuota(SettingsModel map, DomainSettingsKeys key, QuotaEdit quotaEdit) {
		int quota = 0;
		String quotaString = quotaEdit.getStringValue();
		if (null != quotaString) {
			try {
				int quotaAsInt = Integer.parseInt(quotaString);
				quota = quotaAsInt;
			} catch (NumberFormatException | NullPointerException nfe) {
			}
		}
		map.putString(key.name(), String.valueOf(quota));
	}

}
