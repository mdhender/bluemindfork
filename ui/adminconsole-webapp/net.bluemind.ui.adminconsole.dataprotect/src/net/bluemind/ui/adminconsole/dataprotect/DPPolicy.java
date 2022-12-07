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
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

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

	public static final String TYPE = "bm.ac.DPPolicy";

	@UiField
	IntTextEdit daily;

	@UiField
	CrudActionBar actionBar;

	@UiField
	CheckBox backupMails;

	@UiField
	CheckBox backupHSM;

	@UiField
	CheckBox backupES;

	private ScreenRoot instance;

	public DPPolicy(ScreenRoot instance) {
		this.instance = instance;
		HTMLPanel panel = binder.createAndBindUi(this);
		initWidget(panel);

		actionBar.setSaveAction(new ScheduledCommand() {

			@Override
			public void execute() {
				saveClicked();
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {

			@Override
			public void execute() {
				cancelClicked();
			}
		});
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
						Map<String, String> values = new HashMap<String, String>();
						Set<String> skipTags = new HashSet<String>(
								value.stringList(SysConfKeys.dpBackupSkipTags.name()));
						if (!backupMails.getValue().booleanValue()) {
							skipTags.add("mail/imap");
							skipTags.add("mail/archive");
							skipTags.add("mail/cyrus_archives");
						} else {
							skipTags.remove("mail/imap");
							skipTags.remove("mail/archive");
							if (!backupHSM.getValue().booleanValue()) {
								skipTags.add("mail/cyrus_archives");
							} else {
								skipTags.remove("mail/cyrus_archives");
							}
						}

						if (!backupES.getValue().booleanValue()) {
							skipTags.add("bm/es");
						} else {
							skipTags.remove("bm/es");
						}

						values.put(SysConfKeys.dpBackupSkipTags.name(), toString(skipTags));
						sysApi.updateMutableValues(values, new DefaultAsyncHandler<Void>() {
							@Override
							public void success(Void value) {
								Actions.get().showWithParams2("root", new HashMap<String, String>());
							}
						});
					}

					private String toString(Set<String> skipTags) {
						if (skipTags.isEmpty()) {
							return "";
						}
						StringBuffer sb = new StringBuffer();
						for (String tag : skipTags) {
							sb.append(tag + ",");
						}
						sb.deleteCharAt(sb.length() - 1);
						return sb.toString();
					}

				});

			}
		});

	}

	private void cancelClicked() {
		Actions.get().showWithParams2("root", new HashMap<String, String>());
	}

	protected void onScreenShown(ScreenShowRequest ssr) {
		backupMails.setValue(true);
		backupHSM.setValue(false);
		backupMails.addClickHandler(c -> {
			backupHSM.setEnabled(backupMails.getValue().booleanValue());
		});

		DataProtectGwtEndpoint dpApi = new DataProtectGwtEndpoint(Ajax.TOKEN.getSessionId());
		dpApi.getRetentionPolicy(new DefaultAsyncHandler<RetentionPolicy>() {

			@Override
			public void success(RetentionPolicy result) {
				daily.asEditor().setValue(result.daily);
				SystemConfigurationGwtEndpoint sysApi = new SystemConfigurationGwtEndpoint(Ajax.TOKEN.getSessionId());
				sysApi.getValues(new DefaultAsyncHandler<SystemConf>() {

					@Override
					public void success(SystemConf value) {
						backupMails
								.setValue(!value.stringList(SysConfKeys.dpBackupSkipTags.name()).contains("mail/imap"));
						backupHSM.setValue(
								!value.stringList(SysConfKeys.dpBackupSkipTags.name()).contains("mail/cyrus_archives"));
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
