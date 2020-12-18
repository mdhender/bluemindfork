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
package net.bluemind.ui.adminconsole.ldap.importation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasEnabled;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.ProgressDialogPanel;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.scheduledjob.api.JobExecution;
import net.bluemind.scheduledjob.api.JobExecutionQuery;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.api.gwt.endpoint.JobGwtEndpoint;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.api.LdapProperties;
import net.bluemind.system.ldap.importation.api.gwt.endpoint.LdapImportGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.ldap.importation.l10n.Ldap;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.common.client.forms.PasswordEdit;

public class EditDomainLdapEditor extends CompositeGwtWidgetElement {
	static final String TYPE = "bm.ac.EditDomainLdapEditor";

	private static EditDomainLdapUiBinder uiBinder = GWT.create(EditDomainLdapUiBinder.class);

	interface EditDomainLdapUiBinder extends UiBinder<HTMLPanel, EditDomainLdapEditor> {
	}

	@UiField
	CheckBox ldapImportEnabled;

	@UiField
	TextBox ldapHostname;

	@UiField
	ListBox ldapProtocol;

	@UiField
	TextBox ldapBaseDn;

	@UiField
	TextBox ldapLoginDn;

	@UiField
	PasswordEdit ldapLoginPw;

	@UiField
	TextBox ldapUserFilter;

	@UiField
	TextBox ldapGroupFilter;

	@UiField
	TextBox ldapExternalId;

	@UiField
	TextBox ldapSplitDomainGroup;

	@UiField
	Button ldapConnTest;

	@UiField
	Button ldapStartGlobal;

	@UiField
	Button ldapStartIncremental;

	@UiField
	JobStatusPanel lastSyncSuccessful;

	@UiField
	JobStatusPanel lastSyncStatus;

	private List<TextBox> ldapPropertiesTextBox = new ArrayList<>();
	private List<Button> disableOnChange = new ArrayList<>();
	private List<Button> ldapJobActionsButtons = new ArrayList<>();

	private String domainName;
	private String domainUid;
	private boolean propertiesChanged = false;

	protected EditDomainLdapEditor() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);

		ldapPropertiesTextBox.addAll(Arrays.asList(ldapHostname, ldapBaseDn, ldapLoginDn, ldapLoginPw, ldapUserFilter,
				ldapGroupFilter, ldapExternalId, ldapSplitDomainGroup));

		disableOnChange.addAll(Arrays.asList(ldapStartGlobal, ldapStartIncremental));

		if (Ajax.TOKEN.isDomainGlobal()) {
			ldapJobActionsButtons.addAll(Arrays.asList(ldapStartGlobal, ldapStartIncremental));
			addLdapPropertiesChangeHandlers();
		} else {
			ldapJobActionsButtons.addAll(Arrays.asList(ldapStartIncremental));
			ldapStartGlobal.setVisible(false);
		}

		displayLastSyncStatus();
	}

	private void addLdapPropertiesChangeHandlers() {
		for (TextBox ldapProperty : ldapPropertiesTextBox) {
			ldapProperty.addValueChangeHandler(new ValueChangeHandler<String>() {
				@Override
				public void onValueChange(ValueChangeEvent<String> event) {
					propertiesChanged();
				}
			});
		}

		ldapProtocol.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				propertiesChanged();
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsDomain domain = map.get(DomainKeys.domain.name()).cast();
		domainName = domain.getName();
		domainUid = map.getString(DomainKeys.domainUid.name());

		Boolean importEnabled = Boolean.valueOf(domain.getProperties().get(LdapProperties.import_ldap_enabled.name()));
		ldapImportEnabled.setValue(importEnabled);
		enableLdapProperties(importEnabled);

		ldapHostname.setValue(domain.getProperties().get(LdapProperties.import_ldap_hostname.name()));
		loadProtocol(domain.getProperties().get(LdapProperties.import_ldap_protocol.name()),
				Boolean.valueOf(domain.getProperties().get(LdapProperties.import_ldap_accept_certificate.name())));
		ldapBaseDn.setValue(domain.getProperties().get(LdapProperties.import_ldap_base_dn.name()));
		ldapLoginDn.setValue(domain.getProperties().get(LdapProperties.import_ldap_login_dn.name()));
		ldapLoginPw.setValue(domain.getProperties().get(LdapProperties.import_ldap_password.name()));

		String filter = domain.getProperties().get(LdapProperties.import_ldap_user_filter.name());
		if (filter == null || filter.trim().isEmpty()) {
			filter = LdapProperties.import_ldap_user_filter.getDefaultValue();
		}
		ldapUserFilter.setValue(filter);

		filter = domain.getProperties().get(LdapProperties.import_ldap_group_filter.name());
		if (filter == null || filter.trim().isEmpty()) {
			filter = LdapProperties.import_ldap_group_filter.getDefaultValue();
		}
		ldapGroupFilter.setValue(filter);

		ldapExternalId.setValue(domain.getProperties().get(LdapProperties.import_ldap_ext_id_attribute.name()));
		ldapSplitDomainGroup
				.setValue(domain.getProperties().get(LdapProperties.import_ldap_relay_mailbox_group.name()));
	}

	private void loadProtocol(String protocol, boolean allCertificate) {
		if (protocol == null) {
			protocol = "plain";
		}

		switch (protocol.toLowerCase()) {
		case "ssl":
			if (allCertificate) {
				ldapProtocol.setSelectedIndex(4);
			} else {
				ldapProtocol.setSelectedIndex(3);
			}
			break;
		case "tls":
			if (allCertificate) {
				ldapProtocol.setSelectedIndex(2);
			} else {
				ldapProtocol.setSelectedIndex(1);
			}
			break;
		default:
			ldapProtocol.setSelectedIndex(0);
			break;
		}
	}

	private String ldapProtocol() {
		if (ldapProtocol.getSelectedValue() == null) {
			return "plain";
		}

		if (ldapProtocol.getSelectedValue().startsWith("tls")) {
			return "tls";
		}

		if (ldapProtocol.getSelectedValue().startsWith("ssl")) {
			return "ssl";
		}

		return "plain";
	}

	private String ldapAllCertificate() {
		if (ldapProtocol.getSelectedValue() != null && ldapProtocol.getSelectedValue().endsWith("AllCert")) {
			return Boolean.TRUE.toString();
		}

		return Boolean.FALSE.toString();
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsDomain domain = map.get(DomainKeys.domain.name()).cast();

		JsMapStringString domainProperties = domain.getProperties();
		domainProperties.put(LdapProperties.import_ldap_enabled.name(), ldapImportEnabled.getValue().toString());
		domainProperties.put(LdapProperties.import_ldap_hostname.name(), ldapHostname.getValue());
		domainProperties.put(LdapProperties.import_ldap_protocol.name(), ldapProtocol());
		domainProperties.put(LdapProperties.import_ldap_accept_certificate.name(), ldapAllCertificate());
		domainProperties.put(LdapProperties.import_ldap_base_dn.name(), ldapBaseDn.getValue());
		domainProperties.put(LdapProperties.import_ldap_login_dn.name(), ldapLoginDn.getValue());
		domainProperties.put(LdapProperties.import_ldap_password.name(), ldapLoginPw.getValue());
		domainProperties.put(LdapProperties.import_ldap_user_filter.name(), ldapUserFilter.getValue());
		domainProperties.put(LdapProperties.import_ldap_group_filter.name(), ldapGroupFilter.getValue());
		domainProperties.put(LdapProperties.import_ldap_ext_id_attribute.name(), ldapExternalId.getValue());
		domainProperties.put(LdapProperties.import_ldap_relay_mailbox_group.name(), ldapSplitDomainGroup.getValue());
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new EditDomainLdapEditor();
			}
		});
	}

	@UiHandler("ldapImportEnabled")
	void ldapImportChangeHandler(ClickEvent ce) {
		enableLdapProperties(((CheckBox) ce.getSource()).getValue());
	}

	@UiHandler("ldapConnTest")
	void ldapConnTestClickHandler(ClickEvent ce) {
		final ProgressDialogPanel progress = new ProgressDialogPanel();
		progress.setText("Testing...");
		progress.center();
		progress.show();

		new LdapImportGwtEndpoint(Ajax.TOKEN.getSessionId()).testParameters(ldapHostname.getValue(), ldapProtocol(),
				ldapAllCertificate(), ldapBaseDn.getValue(), ldapLoginDn.getValue(), ldapLoginPw.getValue(),
				ldapUserFilter.getValue(), ldapGroupFilter.getValue(), new DefaultAsyncHandler<Void>() {
					@Override
					public void failure(Throwable e) {
						progress.hide();
						Notification.get().reportError(Ldap.INST.fail() + ": " + e.getMessage());
					}

					@Override
					public void success(Void value) {
						progress.hide();
						Notification.get().reportInfo(Ldap.INST.testSuccess());
					}
				});
	}

	@UiHandler("ldapStartIncremental")
	void ldapStartIncrementalClickHandler(ClickEvent ce) {
		new JobGwtEndpoint(Ajax.TOKEN.getSessionId()).start(LdapConstants.JID, domainName, new AsyncHandler<Void>() {
			@Override
			public void success(Void value) {
				Notification.get().reportInfo(Ldap.INST.incrementalStartSuccess());
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(Ldap.INST.incrementalStartFail());
			}
		});
	}

	@UiHandler("ldapStartGlobal")
	void ldapStartGlobalClickHandler(ClickEvent ce) {
		new LdapImportGwtEndpoint(Ajax.TOKEN.getSessionId()).fullSync(domainUid, new AsyncHandler<Void>() {
			@Override
			public void success(Void value) {
				Notification.get().reportInfo(Ldap.INST.globalStartSuccess());
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(Ldap.INST.globalStartFail());
			}
		});
	}

	private void enableLdapProperties(Boolean enabled) {
		if (Ajax.TOKEN.isDomainGlobal()) {
			for (HasEnabled ldapProperty : ldapPropertiesTextBox) {
				ldapProperty.setEnabled(enabled);
			}
			ldapProtocol.setEnabled(enabled);
			ldapImportEnabled.setEnabled(true);
		} else {
			ldapImportEnabled.setEnabled(false);
		}

		ldapConnTest.setEnabled(enabled);
		if (!propertiesChanged) {
			for (Button ldapAction : ldapJobActionsButtons) {
				ldapAction.setEnabled(enabled);
			}
		}
	}

	private void displayLastSyncStatus() {
		JobExecutionQuery jeq = new JobExecutionQuery();
		jeq.domain = domainName;
		jeq.jobId = LdapConstants.JID;
		jeq.from = 0;
		jeq.size = 1;

		new JobGwtEndpoint(Ajax.TOKEN.getSessionId()).searchExecution(jeq,
				new AsyncHandler<ListResult<JobExecution>>() {
					@Override
					public void success(ListResult<JobExecution> jel) {
						if (jel.total == 0) {
							setUnknownStatus(lastSyncStatus);
							return;
						}

						fillElement(lastSyncStatus, jel.values.get(0));
					}

					@Override
					public void failure(Throwable e) {
						setUnknownStatus(lastSyncStatus);
					}
				});

		jeq.statuses.clear();
		jeq.statuses.add(JobExitStatus.SUCCESS);
		new JobGwtEndpoint(Ajax.TOKEN.getSessionId()).searchExecution(jeq,
				new AsyncHandler<ListResult<JobExecution>>() {
					@Override
					public void success(ListResult<JobExecution> jel) {
						if (jel.total == 0) {
							setUnknownStatus(lastSyncSuccessful);
							return;
						}

						fillElement(lastSyncSuccessful, jel.values.get(0));
					}

					@Override
					public void failure(Throwable e) {
						setUnknownStatus(lastSyncSuccessful);
					}
				});
	}

	private void fillElement(JobStatusPanel element, JobExecution jobExecution) {
		element.add(getIcon(jobExecution.status));
		element.add(new InlineHTML(
				DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_MEDIUM).format(jobExecution.startDate)));
		element.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				HashMap<String, String> req = getJobExecutionLink();
				Actions.get().showWithParams2("editJob", req);
			}
		});
	}

	private void setUnknownStatus(JobStatusPanel element) {
		Label label = new Label();
		label.setStyleName("fa fa-circle-thin fa-lg");
		element.add(label);
		element.add(new InlineHTML(Ldap.INST.unknownLastSync()));
		element.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				HashMap<String, String> req = getJobExecutionLink();
				Actions.get().showWithParams2("editJob", req);
			}
		});
	}

	@SuppressWarnings("incomplete-switch")
	private Label getIcon(JobExitStatus status) {
		Label label = new Label();
		String style = "fa fa-circle-thin fa-lg";
		switch (status) {
		case SUCCESS:
			style = "fa fa-check fa-lg";
			break;
		case FAILURE:
			style = "fa fa-close fa-lg";
			break;
		case COMPLETED_WITH_WARNINGS:
			style = "fa fa-warning fa-lg";
			break;
		}
		label.setStyleName(style);
		return label;
	}

	private HashMap<String, String> getJobExecutionLink() {
		HashMap<String, String> req = new HashMap<>();
		req.put("jobId", LdapConstants.JID);
		req.put("domain", domainName);
		req.put("activeTab", "2");
		return req;
	}

	private void propertiesChanged() {
		propertiesChanged = true;

		for (Button ldapAction : disableOnChange) {
			ldapAction.setEnabled(false);
			ldapAction.setTitle(Ldap.INST.buttonTipDisbledOnChange());
		}
	}
}
