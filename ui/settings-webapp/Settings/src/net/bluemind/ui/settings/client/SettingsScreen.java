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
package net.bluemind.ui.settings.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.menus.Menus;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.settings.client.forms.SettingsActionBar;

public class SettingsScreen extends Composite implements IGwtCompositeScreenRoot {
	private static RootScreenUiBinder uiBinder = GWT.create(RootScreenUiBinder.class);

	interface RootScreenUiBinder extends UiBinder<DockLayoutPanel, SettingsScreen> {
	}

	private final Map<String, AppAnchor> sectionAnchors = new HashMap<>();

	@UiField
	FlexTable sidebar;

	@UiField
	SimplePanel south;

	@UiField
	FlowPanel apps;

	private SettingsActionBar acitonBar = new SettingsActionBar();
	private ScreenRoot instance;
	private JavaScriptObject model;
	private SettingsConstants constants;

	public static interface SettingsConstants extends Constants {

		String settingsUpdated();

	}

	public SettingsScreen(ScreenRoot screenRoot) {
		this.instance = screenRoot;
		constants = GWT.create(SettingsConstants.class);

		initWidget(uiBinder.createAndBindUi(this));

		sidebar.setStyleName("sidebar");
		sidebar.setWidget(0, 0, new Label());
		sidebar.getRowFormatter().setStyleName(0, "fakeapp");

		initSections();

		south.add(acitonBar);
		south.setStyleName("actionBar");

		acitonBar.getSaveBtn().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				save();
			}
		});

		History.addValueChangeHandler(new ValueChangeHandler<String>() {

			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				locationUpdated();
			}
		});

	}

	private void initSections() {
		Menus menus = new Menus("net.bluemind.ui.settings.menusContributor");

		for (Section s : menus.getRootAsList()) {
			registerSection(s);
		}

	}

	private void registerSection(final Section screen) {
		if (!Menus.isInRoles(screen.getRoles())) {
			GWT.log("not in roles " + screen.getRoles());
			return;
		}
		int row = sidebar.getRowCount();
		AppAnchor anchor = new AppAnchor(screen.getName(), screen.getId());
		sidebar.setWidget(row, 0, anchor);

		sidebar.getRowFormatter().addStyleName(row, "app");

		sectionAnchors.put(screen.getId(), anchor);
		anchor.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				updateLocation(screen.getId());
			}

		});

	}

	private void showApp(String id) {
		GWT.log("show app " + id);
		for (String a : sectionAnchors.keySet()) {

			boolean selected = a.equals(id);
			sectionAnchors.get(a).setSelected(selected);

		}

		RootScreen.show(instance.getContent(), id);
	}

	protected void save() {
		instance.save(new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				Notification.get().reportInfo(constants.settingsUpdated());
				load();
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	public void load() {
		instance.load(new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
				locationUpdated();
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	@Override
	public Element getCenter() {
		return apps.getElement();
	}

	@Override
	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();

		Event.setEventListener(getElement(), new EventListener() {

			@Override
			public void onBrowserEvent(Event event) {
				instance.saveModel(instance.getModel());
				instance.loadModel(instance.getModel());
			}
		});

		DOM.sinkBitlessEvent(getElement(), "refresh");

	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	@Override
	public void doLoad(ScreenRoot instance) {
		load();
	}

	public native static ScreenRoot create(String id)
	/*-{
		return {
			'id' : id,
			'type' : "bm.settings.SettingsScreen",
			'modelHandlers' : []
		};
	}-*/;

	public static void registerType() {
		GwtScreenRoot.registerComposite("bm.settings.SettingsScreen",
				new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

					@Override
					public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
						return new SettingsScreen(screenRoot);
					}
				});
	}

	private void locationUpdated() {
		String token = History.getToken();
		GWT.log("token " + token);
		if (token == null || token.isEmpty()) {
			showApp("myAccount");
		} else {
			String module = "myAccount";
			for (String a : sectionAnchors.keySet()) {
				if (a.equals(token)) {
					module = a;
					break;
				}
			}
			showApp(module);
		}
	}

	private void updateLocation(String value) {
		History.newItem(value);
	}
}
