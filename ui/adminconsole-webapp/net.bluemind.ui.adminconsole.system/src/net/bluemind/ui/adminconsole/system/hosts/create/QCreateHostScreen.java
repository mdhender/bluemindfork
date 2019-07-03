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
package net.bluemind.ui.adminconsole.system.hosts.create;

import java.util.HashMap;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.QuickCreateActionBar;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;

public class QCreateHostScreen extends Composite implements IGwtCompositeScreenRoot {

	interface QCreateHostUiBinder extends UiBinder<DockLayoutPanel, QCreateHostScreen> {
	}

	public static final String TYPE = "bm.ac.QCreateHostScreen";

	private static QCreateHostUiBinder uiBinder = GWT.create(QCreateHostUiBinder.class);

	@UiField
	QuickCreateActionBar actionBar;

	@UiField
	Label center;

	@UiField
	Label errorLabel;

	private DockLayoutPanel dlp;
	private ScreenRoot instance;

	private QCreateHostScreen(ScreenRoot screenRoot) {
		this.instance = screenRoot;
		dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		actionBar.setCreateAction(new ScheduledCommand() {
			@Override
			public void execute() {

				instance.save(new AsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						Actions.get().showWithParams2("hosts", null);
					}

					@Override
					public void failure(Throwable e) {
						errorLabel.setText(e.getMessage());
					}
				});
			}
		});

		actionBar.setCreateAndEditAction(new ScheduledCommand() {
			@Override
			public void execute() {

				instance.save(new AsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						String hostId = instance.getModel().<JsMapStringJsObject> cast()
								.getString(HostKeys.host.name());
						HashMap<String, String> params = new HashMap<>();
						params.put(HostKeys.host.name(), hostId);
						Actions.get().showWithParams2("editHost", params);
					}

					@Override
					public void failure(Throwable e) {
						errorLabel.setText(e.getMessage());
					}
				});
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {
			@Override
			public void execute() {
				Actions.get().showWithParams2("hosts", null);
			}
		});
	}

	@Override
	public Element getCenter() {
		return center.getElement();
	}

	@Override
	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public QCreateHostScreen create(ScreenRoot instance) {
				return new QCreateHostScreen(instance);
			}
		});
		GWT.log("bm.ac.QCreateHostScreen registered");
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
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
				// cannot fail
			}
		});
	}

}
