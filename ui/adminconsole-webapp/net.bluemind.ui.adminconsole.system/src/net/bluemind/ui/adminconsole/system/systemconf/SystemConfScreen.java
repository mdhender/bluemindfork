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
package net.bluemind.ui.adminconsole.system.systemconf;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.CrudActionBar;
import net.bluemind.ui.adminconsole.system.systemconf.auth.SysConfAuthenticationEditor;
import net.bluemind.ui.adminconsole.system.systemconf.auth.l10n.SysConfAuthConstants;
import net.bluemind.ui.adminconsole.system.systemconf.eas.SysConfEasServerEditor;
import net.bluemind.ui.adminconsole.system.systemconf.eas.l10n.SysConfEasConstants;
import net.bluemind.ui.adminconsole.system.systemconf.mail.SysConfMailEditor;
import net.bluemind.ui.adminconsole.system.systemconf.mail.l10n.SysConfMailConstants;
import net.bluemind.ui.adminconsole.system.systemconf.reverseProxy.SysConfReverseProxyEditor;
import net.bluemind.ui.adminconsole.system.systemconf.reverseProxy.l10n.SysConfReverseProxyConstants;

public class SystemConfScreen extends Composite implements IGwtCompositeScreenRoot {

	public static final String TYPE = "bm.ac.SystemConfScreen";
	@UiField
	SimplePanel center;

	@UiField
	CrudActionBar actionBar;

	private ScreenRoot screenRoot;

	interface SystemConfUiBinder extends UiBinder<DockLayoutPanel, SystemConfScreen> {
	}

	private static SystemConfUiBinder uiBinder = GWT.create(SystemConfUiBinder.class);

	private SystemConfScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		actionBar.setCancelAction(getCancelAction());
		actionBar.setSaveAction(getSaveAction());
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new SystemConfScreen(screenRoot);
			}
		});
	}

	public Element getCenter() {
		return center.getElement();
	}

	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
		instance.load(new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	private ScheduledCommand getSaveAction() {
		return new ScheduledCommand() {

			@Override
			public void execute() {
				screenRoot.save(new DefaultAsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						Actions.get().showWithParams2("system", null);
					}
				});
			}
		};
	}

	private ScheduledCommand getCancelAction() {
		return new ScheduledCommand() {

			@Override
			public void execute() {
				History.back();
			}
		};
	}

	public static ScreenElement screenModel() {

		ScreenRoot screenRoot = ScreenRoot.create("systemConf", SystemConfScreen.TYPE).cast();

		screenRoot.getHandlers().push(ModelHandler.create(null, SysConfModelHandler.TYPE).cast());
		screenRoot.getHandlers().push(ModelHandler.create(null, GlobalSettingsModelHandler.TYPE).cast());

		JsArray<Tab> tabs = JsArray.createArray().cast();

		tabs.push(Tab.create(null, SysConfMailConstants.INST.mail(),
				ScreenElement.create("sysConfMail", SysConfMailEditor.TYPE)));

		tabs.push(Tab.create(null, SysConfReverseProxyConstants.INST.reverseProxyTab(),
				ScreenElement.create("sysConfReverseProxy", SysConfReverseProxyEditor.TYPE)));

		tabs.push(Tab.create(null, SysConfEasConstants.INST.easTab(),
				ScreenElement.create("sysConfEasServer", SysConfEasServerEditor.TYPE)));

		tabs.push(Tab.create(null, SysConfAuthConstants.INST.authTab(),
				ScreenElement.create("sysConfAuthentication", SysConfAuthenticationEditor.TYPE)));

		TabContainer tab = TabContainer.create("editSystemConfTabs", tabs);
		screenRoot.setContent(tab);
		return screenRoot;
	}

}
