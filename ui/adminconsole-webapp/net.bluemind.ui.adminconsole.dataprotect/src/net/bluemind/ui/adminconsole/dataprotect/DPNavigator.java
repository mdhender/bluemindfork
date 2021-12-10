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

import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.VersionInfo;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.gwt.endpoint.DataProtectGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.gwt.endpoint.InstallationGwtEndpoint;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.adminconsole.dataprotect.l10n.DPTexts;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.TablePanel;
import net.bluemind.ui.common.client.forms.TrPanel;

public class DPNavigator extends Composite implements IGwtScreenRoot {

	interface DPNavigatorUi extends UiBinder<HTMLPanel, DPNavigator> {

	}

	public static final String TYPE = "bm.ac.DPNavigator";

	private DPNavigatorUi binder = GWT.create(DPNavigatorUi.class);

	private HTMLPanel panel;

	@UiField
	DivElement contentDiv;

	@UiField
	TablePanel table;

	@UiField
	PushButton sync;

	@UiField
	Label syncLabel;

	private ScreenRoot instance;

	public DPNavigator(ScreenRoot instance) {
		this.instance = instance;
		this.panel = binder.createAndBindUi(this);
		initWidget(panel);
	}

	@UiHandler("sync")
	public void handleRestart(ClickEvent e) {
		DataProtectGwtEndpoint dpApi = new DataProtectGwtEndpoint(Ajax.TOKEN.getSessionId());
		dpApi.syncWithFilesystem(new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				Window.Location.reload();
			}
		});
	}

	protected void onScreenShown(ScreenShowRequest ssr) {
		if (!Ajax.TOKEN.isDomainGlobal()) {
			sync.setVisible(false);
			syncLabel.setVisible(false);
		}

		final InstallationGwtEndpoint installationApi = new InstallationGwtEndpoint(Ajax.TOKEN.getSessionId());

		installationApi.getSubscriptionInformations(new AsyncHandler<SubscriptionInformations>() {

			@Override
			public void success(SubscriptionInformations value) {
				detectVersion(installationApi, value.validProductiveLicense());
			}

			@Override
			public void failure(Throwable e) {
				detectVersion(installationApi, false);
			}

			private void detectVersion(final InstallationGwtEndpoint installationApi, final boolean licensePresent) {
				installationApi.getVersion(new DefaultAsyncHandler<InstallationVersion>() {

					@Override
					public void success(InstallationVersion version) {

						final VersionInfo currentVersion = VersionInfo.create(version.softwareVersion,
								version.versionName);
						DataProtectGwtEndpoint dpApi = new DataProtectGwtEndpoint(Ajax.TOKEN.getSessionId());
						dpApi.getAvailableGenerations(new DefaultAsyncHandler<List<DataProtectGeneration>>() {

							@Override
							public void success(List<DataProtectGeneration> value) {
								showGens(value, currentVersion, licensePresent);
							}

						});

					}
				});
			}

		});

	}

	private void showGens(List<DataProtectGeneration> gens, VersionInfo currentVersion, boolean licensePresent) {
		// most recent gen is last in list
		final int len = gens.size() - 1;
		GWT.log("showing " + len + " generations.");
		for (int i = len; i >= 0; i--) {
			DataProtectGeneration dpg = gens.get(i);
			TrPanel tr = new GenerationRow(dpg, currentVersion, licensePresent);
			table.add(tr);
		}
	}

	@UiFactory
	public DPTexts texts() {
		return DPTexts.INST;
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new DPNavigator(screenRoot);
			}
		});

	}

	@Override
	public void attach(Element e) {
		GWT.log("dpn on attach");
		DOM.appendChild(e, getElement());
		onScreenShown(new ScreenShowRequest());
		onAttach();
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		GWT.log("dpn load model");

	}

	@Override
	public void saveModel(JavaScriptObject model) {
		GWT.log("dpn save model");
		// TODO Auto-generated method stub

	}

	@Override
	public void doLoad(ScreenRoot instance) {
		GWT.log("dpn doLoad " + instance);

	}
}
