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
package net.bluemind.ui.adminconsole.security.iptables;

import java.util.HashMap;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.gwt.endpoint.SecurityMgmtGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.CrudActionBar;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.StringEdit;

public class IpTablesEditor extends CompositeGwtWidgetElement implements IGwtCompositeScreenRoot {
	public static final String TYPE = "bm.ac.IpTablesEditor";

	private ScreenRoot instance;

	@UiField
	HTMLPanel center;

	@UiField
	StringEdit additionalIPs;

	@UiField
	CrudActionBar actionBar;

	interface IpTablesUiBinder extends UiBinder<DockLayoutPanel, IpTablesEditor> {
	}

	private static IpTablesUiBinder uiBinder = GWT.create(IpTablesUiBinder.class);

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new IpTablesEditor(screenRoot);
			}
		});
	}

	private IpTablesEditor(ScreenRoot screenRoot) {
		this.instance = screenRoot;
		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		actionBar.setSaveAction(new ScheduledCommand() {
			@Override
			public void execute() {
				doSave();
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {
			@Override
			public void execute() {
				doCancel();
			}
		});
	}

	protected void doSave() {
		instance.save(new DefaultAsyncHandler<Void>() {
			@Override
			public void success(Void value) {
				doFirewallUpdate();
			}
		});
	}

	private void doFirewallUpdate() {
		new SecurityMgmtGwtEndpoint(Ajax.TOKEN.getSessionId()).updateFirewallRules(new DefaultAsyncHandler<TaskRef>() {
			@Override
			public void success(TaskRef value) {
				Notification.get().reportInfo("saved");
				HashMap<String, String> ssr = new HashMap<>();
				ssr.put("task", value.id + "");
				ssr.put("pictures", null);
				ssr.put("return", "security");
				ssr.put("success", "security");
				Actions.get().showWithParams2("progress", ssr);
			}
		});
	}

	protected void doCancel() {
		History.back();
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsMapStringString values = map.get("sysconf").cast();
		values.put(SysConfKeys.fwAdditionalIPs.name(), additionalIPs.getStringValue());
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsMapStringString values = map.get("sysconf").cast();
		additionalIPs.setStringValue(values.get(SysConfKeys.fwAdditionalIPs.name()));
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
		instance.load(new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	@Override
	public Element getCenter() {
		return center.getElement();
	}
}
