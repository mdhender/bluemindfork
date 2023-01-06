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
package net.bluemind.ui.adminconsole.dataprotect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IntegerBox;

import net.bluemind.dataprotect.api.RetentionPolicy;
import net.bluemind.dataprotect.api.gwt.endpoint.DataProtectGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.api.gwt.endpoint.SystemConfigurationGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.IntTextEdit;

public class DPPolicy extends Composite implements IGwtScreenRoot {

	interface DPPolicyUI extends UiBinder<HTMLPanel, DPPolicy> {

	}

	private static final DPPolicyUI binder = GWT.create(DPPolicyUI.class);
	final String _SDS_BACKUP_RETENTION_DAYS_DEFAULT = "90";
	
	public static final String TYPE = "bm.ac.DPPolicy";

	@UiField
	IntTextEdit daily;
	
	@UiField
	IntTextEdit retentionDays;

	@UiField
	CrudActionBar actionBar;

	@UiField
	CheckBox backupMails;

	@UiField
	CheckBox backupES;

	public DPPolicy(ScreenRoot instance) {
		HTMLPanel panel = binder.createAndBindUi(this);
		initWidget(panel);

		actionBar.setSaveAction(this::saveClicked);
		actionBar.setCancelAction(this::cancelClicked);
	}

	private void saveClicked() {
		RetentionPolicy rp = new RetentionPolicy();
		rp.daily = daily.asEditor().getValue();
		DataProtectGwtEndpoint dpApi = new DataProtectGwtEndpoint(Ajax.TOKEN.getSessionId());
		dpApi.updatePolicy(rp, new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				final SystemConfigurationGwtEndpoint sysApi = new SystemConfigurationGwtEndpoint(
						Ajax.TOKEN.getSessionId());

				sysApi.getValues(new DefaultAsyncHandler<SystemConf>() {

					@Override
					public void success(SystemConf value) {
						Map<String, String> values = new HashMap<>();
						Set<String> skipDataTypes = new HashSet<>(
								value.stringList(SysConfKeys.dataprotect_skip_datatypes.name()));
						Set<String> skipTags = new HashSet<>(value.stringList(SysConfKeys.dpBackupSkipTags.name()));
						if (!backupMails.getValue().booleanValue()) {
							skipDataTypes.add("sds-spool");
						} else {
							skipDataTypes.remove("sds-spool");
						}

						if (!backupES.getValue().booleanValue()) {
							skipTags.add("bm/es");
						} else {
							skipTags.remove("bm/es");
						}
						

						values.put(SysConfKeys.dpBackupSkipTags.name(), toString(skipTags));
						values.put(SysConfKeys.dataprotect_skip_datatypes.name(), toString(skipDataTypes));
						values.put(SysConfKeys.sds_backup_rentention_days.name(), retentionDays.getStringValue());
						sysApi.updateMutableValues(values, new DefaultAsyncHandler<Void>() {
							@Override
							public void success(Void value) {
								Actions.get().showWithParams2("root", new HashMap<>());
							}
						});
					}

					private String toString(Set<String> skipTags) {
						return skipTags.stream().collect(Collectors.joining(","));
					}

				});

			}
		});

	}

	private void cancelClicked() {
		Actions.get().showWithParams2("root", new HashMap<>());
	}

	protected void onScreenShown(ScreenShowRequest ssr) {
		backupMails.setValue(true);

		DataProtectGwtEndpoint dpApi = new DataProtectGwtEndpoint(Ajax.TOKEN.getSessionId());
		dpApi.getRetentionPolicy(new DefaultAsyncHandler<RetentionPolicy>() {

			@Override
			public void success(RetentionPolicy result) {
				daily.asEditor().setValue(result.daily);
				SystemConfigurationGwtEndpoint sysApi = new SystemConfigurationGwtEndpoint(Ajax.TOKEN.getSessionId());
				sysApi.getValues(new DefaultAsyncHandler<SystemConf>() {

					@Override
					public void success(SystemConf value) {
						retentionDays.setStringValue(Optional.ofNullable(value.stringValue(SysConfKeys.sds_backup_rentention_days.name()))
								.orElse(_SDS_BACKUP_RETENTION_DAYS_DEFAULT));
						backupMails.setValue(
								!value.stringList(SysConfKeys.dataprotect_skip_datatypes.name()).contains("sds-spool"));
						backupES.setValue(!value.stringList(SysConfKeys.dpBackupSkipTags.name()).contains("bm/es"));

					}
				});
			}
		});
	}

	@Override
	public void attach(Element e) {
		DOM.appendChild(e, getElement());
		onScreenShown(new ScreenShowRequest());
		onAttach();
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doLoad(ScreenRoot instance) {
		// TODO Auto-generated method stub

	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new DPPolicy(screenRoot);
			}
		});

	}

}
