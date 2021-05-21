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
package net.bluemind.ui.adminconsole.base.client;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;

import net.bluemind.directory.api.IDirectoryAsync;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.EditorContext;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElements;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.eventbus.GwtEventBus;
import net.bluemind.gwtconsoleapp.base.eventbus.NotificationEvent;
import net.bluemind.gwtconsoleapp.base.eventbus.NotificationEventHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.IWebAction;
import net.bluemind.ui.adminconsole.base.ui.AppScreen;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.common.client.OverlayScreen;
import net.bluemind.ui.common.client.SizeHint;

/**
 * Admin Console controller.
 * 
 * Manages switch between {@link AppScreen}
 * 
 * Acts as an event bus for between ui & model.
 * 
 * 
 */
public final class AdminCtrl {

	int notifId = 0;
	private ScreenElements screenElements;

	public class SectionAction implements IWebAction {

		private Section section;

		public SectionAction(Section section) {
			this.section = section;
		}

		@Override
		public void run(String path, Map<String, String> params) {
			doSectionAction(path, params);
			updateBreadCrumb(Arrays.asList(section), null);
			mainScreen.notifySectionAction(section.getId());
		}

	}

	public class ScreenAction implements IWebAction {

		private List<Section> sectionsPath;
		private Screen screen;

		public ScreenAction(List<Section> lsection, Screen screen) {
			sectionsPath = lsection;
			this.screen = screen;
		}

		@Override
		public void run(String path, Map<String, String> params) {
			doScreenAction(screen, path, params);
			updateBreadCrumb(sectionsPath, screen);
		}

	}

	public void init() {

		bc = new BreadCrumb();
		screenElements = new ScreenElements("net.bluemind.ui.adminconsole.screenContributor");
		screenElements.register(RootScreen.model());

		// register actions
		Actions.get().registerAction("root", new IWebAction() {

			@Override
			public void run(String path, Map<String, String> params) {
				showScreen(RootScreen.model(), new HashMap<String, String>(), null);
				updateBreadCrumb(Arrays.<Section> asList(), null);
				mainScreen.notifySectionAction(null);
			}
		});

		for (Section section : AdminConsoleMenus.get().getRootAsList()) {
			screenElements.register(SectionScreen.create(section));
			Actions.get().registerAction(section.getId(), new SectionAction(section));
			registerScreens(null, section);

			for (int j = 0; j < section.getSections().length(); j++) {
				Section subSection = section.getSections().get(j);
				registerScreens(section, subSection);
			}

		}

		Actions actions = Actions.get();
		for (String s : screenElements.screens()) {
			if (s != null && !actions.isKnown(s)) {
				GWT.log("Action '" + s + "' is not registered, forcing registration.");
				Screen theScreen = Screen.create(s, s, "", false);
				actions.registerAction(s, new ScreenAction(new LinkedList<Section>(), theScreen));
			}
		}

		spinner = new Spinner();
		spinner.setVisible(false);
		mainScreen = new AdminScreen(AdminConsoleMenus.get().getRootAsList(), bc);
		RootLayoutPanel rlp = RootLayoutPanel.get();
		rlp.clear();
		rlp.getElement().addClassName("root-layout");
		// RootPanel.get().add(spinner);
		rlp.add(spinner);
		rlp.add(mainScreen);

		Notification.exportNotificationFunction();
		registerNotificationGWTLogHandler();

		mainScreen.onScreenShown();

	}

	private void registerNotificationGWTLogHandler() {
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
	}

	private void registerScreens(Section parent, Section section) {

		List<Section> lsection = null;
		if (parent != null) {
			lsection = Arrays.asList(parent, section);
		} else {
			lsection = Arrays.asList(section);
		}

		for (int i = 0; i < section.getScreens().length(); i++) {
			Screen screenElem = section.getScreens().get(i);
			String appId = screenElem.getId();
			Actions.get().registerAction(appId, new ScreenAction(lsection, screenElem));
		}
	}

	private ScreenRoot current;
	private BreadCrumb bc;
	private AdminScreen mainScreen;
	private Spinner spinner;
	private DialogBox overlay;

	public AdminCtrl() {
		init();
	}

	private void doSectionAction(String actionId, Map<String, String> params) {
		ScreenElement elt = screenElements.getElement(actionId);

		showScreen(elt, params, null);
	}

	private void doScreenAction(final Screen screen, String actionId, final Map<String, String> params) {
		final ScreenElement elt = screenElements.getElement(screen.getId());

		if (screen.isDirEntryEditor()) {
			final String domainUid = params.get("domainUid");
			final String dirEntryUid = params.get("entryUid");
			IDirectoryAsync dir = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
			dir.getRolesForDirEntry(dirEntryUid, new DefaultAsyncHandler<Set<String>>() {

				@Override
				public void success(Set<String> value) {
					showScreen(elt, params, value);
				}
			});
		} else {

			GWT.log("History state: [action:" + actionId + "] [query:" + params + "]");
			if (elt == null) {
				GWT.log("no action found for " + params);
				return;
			}

			HashSet<String> roles = new HashSet<>(Ajax.TOKEN.getRoles());
			showScreen(elt, params, roles);
		}
	}

	private void showScreen(ScreenElement elt, Map<String, String> params, Set<String> roles) {
		if (roles == null) {
			roles = new HashSet<>(Ajax.TOKEN.getRoles());
		}
		EditorContext screenContext = EditorContext.create(roles);

		elt = ScreenRoot.build(elt.<ScreenRoot> cast(), params, screenContext);
		if (overlay != null) {
			overlay.hide();
			overlay = null;
		}

		if (current != null) {
			mainScreen.centerPanel.clear();
			current = null;
		}

		// FIXME should call elt.castElement("WidgetElement");
		ScreenRoot we = elt.cast();
		current = we;
		if (we.isOverlay()) {

			SizeHint sh = new SizeHint(OverlayScreen.DEF_WIDTH, OverlayScreen.DEF_HEIGHT);
			if (we.getSizeHint() != null) {
				sh = new SizeHint(we.getSizeHint().getWidth(), we.getSizeHint().getHeight());
			}
			Label t = new Label();
			t.setWidth("" + sh.getWidth() + "px");
			t.setHeight("" + sh.getHeight() + "px");
			overlay = new DialogBox();
			overlay.addStyleName("dialog");

			overlay.setWidget(t);
			overlay.setGlassEnabled(true);
			overlay.setAutoHideEnabled(false);
			overlay.setGlassStyleName("modalOverlay");
			overlay.setModal(true);
			overlay.center();
			overlay.show();
			we.attach(t.getElement());
		} else {
			FlowPanel t = new FlowPanel();
			t.setHeight("100%");
			mainScreen.centerPanel.add(t);
			mainScreen.centerPanel.showWidget(t);
			we.attach(t.getElement());
		}

	}

	private void updateBreadCrumb(List<Section> sections, final Screen screen) {
		bc.clearCrumb();

		for (Section section : sections) {
			final String sectionId = section.getId();
			ClickHandler sectionClicked = new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Actions.get().show(sectionId, new ScreenShowRequest());
				}
			};

			Anchor anchor = new Anchor(section.getName());

			bc.add(anchor, sectionClicked);

		}

		if (screen != null) {

			ClickHandler sectionClicked = new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Actions.get().show(screen.getId(), new ScreenShowRequest());
				}
			};

			Anchor anchor = new Anchor(screen.getName());
			bc.add(anchor, sectionClicked);
		}

	}

	public BreadCrumb getBreadCrumb() {
		return bc;
	}

}
