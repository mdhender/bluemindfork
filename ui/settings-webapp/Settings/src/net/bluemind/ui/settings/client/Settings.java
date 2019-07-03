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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;

import net.bluemind.gwtconsoleapp.base.editor.BasePlugin;
import net.bluemind.gwtconsoleapp.base.editor.EditorContext;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElements;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.eventbus.GwtEventBus;
import net.bluemind.gwtconsoleapp.base.eventbus.NotificationEvent;
import net.bluemind.gwtconsoleapp.base.eventbus.NotificationEventHandler;
import net.bluemind.gwtconsoleapp.base.lifecycle.GwtAppLifeCycle;
import net.bluemind.gwtconsoleapp.base.lifecycle.ILifeCycle;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.restbus.api.gwt.RestBusImpl;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.settings.addressbook.AddressBookSettingsPlugin;
import net.bluemind.ui.settings.calendar.CalendarSettingsPlugin;
import net.bluemind.ui.settings.client.about.AboutPanel;
import net.bluemind.ui.settings.client.forms.apikeys.AKPanel;
import net.bluemind.ui.settings.client.myaccount.MyAccountPlugin;
import net.bluemind.ui.settings.mail.MailSettingsPlugin;
import net.bluemind.ui.settings.task.TaskSettingsPlugin;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Settings implements EntryPoint {

	public boolean alreadyStarted = false;

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		BasePlugin.install();

		RootScreen.registerType();
		SettingsScreen.registerType();
		AboutPanel.registerType();
		AKPanel.registerType();
		MyAccountPlugin.install();

		ScreenElementContributor.exportAsfunction("gwtSettingsMainScreensContributor",
				ScreenElementContributor.create(new MainScreensContributor()));

		MenuContributor.exportAsfunction("gwtSettingsMainMenusContributor",
				MenuContributor.create(new MainMenusContributor()));

		MailSettingsPlugin.install();
		CalendarSettingsPlugin.install();
		AddressBookSettingsPlugin.install();
		TaskSettingsPlugin.install();

		GwtAppLifeCycle.registerLifeCycle("net.bluemind.ui.settings", new ILifeCycle() {

			@Override
			public void start() {
				RestBusImpl.get().addListener(o -> {
					if (o && !alreadyStarted) {
						alreadyStarted = true;
						startUp();
					}
				});
			}
		});

	}

	protected void startUp() {

		RootLayoutPanel rlp = RootLayoutPanel.get();

		FlowPanel root = new FlowPanel();
		root.setHeight("100%");
		rlp.clear();

		rlp.add(root);

		ScreenElements screenElements = new ScreenElements("net.bluemind.ui.settings.screensContributor");
		ScreenElement elt = screenElements.getElement("base");
		HashMap<String, String> t = new HashMap<>();
		t.put("userId", Ajax.TOKEN.getSubject());
		t.put("entryUid", Ajax.TOKEN.getSubject());
		t.put("mailboxUid", Ajax.TOKEN.getSubject());
		t.put("domainUid", Ajax.TOKEN.getContainerUid());

		ScreenRoot screenRoot = ScreenRoot.build(elt.<ScreenRoot> cast(), t,
				EditorContext.create(Ajax.TOKEN.getRoles()));
		screenRoot.attach(root.getElement());
		Notification.exportNotificationFunction();
		NotificationPanel panel = new NotificationPanel();
		registerNotificationHandler(panel);
		RootPanel.get().add(panel);
	}

	private void registerNotificationHandler(final NotificationPanel notification) {
		GwtEventBus.bus.addHandler(NotificationEvent.TYPE, new NotificationEventHandler() {
			@Override
			public void onNotify(NotificationEvent event) {
				switch (event.notificationType) {
				case INFO:
					GWT.log("INFO: " + event.message);
					break;
				case ERROR:
				case EXCEPTION:
					GWT.log("ERROR: " + event.message);
				}
			}
		});
		GwtEventBus.bus.addHandler(NotificationEvent.TYPE, new NotificationEventHandler() {
			@Override
			public void onNotify(NotificationEvent event) {
				switch (event.notificationType) {
				case INFO:
					notification.showOk(event.message);
					break;
				case ERROR:
					notification.showError(event.message);
					break;
				case EXCEPTION:
					notification.showError(event.exception.getMessage());
				}
			}
		});
	}
}
