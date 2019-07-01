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
package net.bluemind.ui.adminconsole.system.hosts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.gwt.endpoint.ServerGwtEndpoint;
import net.bluemind.ui.adminconsole.base.ui.MenuButton.PopupOrientation;
import net.bluemind.ui.adminconsole.system.hosts.l10n.HostConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class HostsScreen extends Composite implements IGwtScreenRoot {

	private DockLayoutPanel dlp;

	private static HostConstants constants = HostConstants.INST;

	@UiField
	HostsGrid grid;

	@UiField
	Button deleteButton;

	@UiField
	SimplePanel newButtonContainer;

	@UiHandler("deleteButton")
	void deleteClick(ClickEvent e) {
		Collection<ItemValue<Server>> selection = grid.getSelected();
		List<ItemValue<Server>> listSelection = new ArrayList<ItemValue<Server>>(selection);

		String confirm = constants.deleteConfirmation();
		if (selection.size() > 1) {
			confirm = constants.massDeleteConfirmation();
		}

		deleteServers(listSelection, confirm);
	}

	interface HostsUiBinder extends UiBinder<DockLayoutPanel, HostsScreen> {
	}

	private static HostsUiBinder uiBinder = GWT.create(HostsUiBinder.class);

	public interface BBBundle extends ClientBundle {
		@Source("HostsScreen.css")
		BBStyle getStyle();
	}

	public interface BBStyle extends CssResource {
		String newButton();
	}

	public static final BBBundle bundle;
	public static final BBStyle style;

	public static final String TYPE = "bm.ac.HostsScreen";

	static {
		bundle = GWT.create(BBBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();
	}

	private HostsScreen(ScreenRoot screenRoot) {
		this.dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);
		dlp.setHeight("100%");
		NewButton newButton = new NewButton(constants.newButton(), PopupOrientation.DownRight);
		newButton.addStyleName("primary");
		newButton.addStyleName(style.newButton());
		newButtonContainer.add(newButton);

		Handler handler = new Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				Collection<ItemValue<Server>> entries = grid.getSelected();
				deleteButton.setEnabled(!entries.isEmpty());
			}
		};
		grid.addSelectionChangeHandler(handler);

	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new HostsScreen(screenRoot);
			}
		});
	}

	public void attach(Element parent) {
		parent.appendChild(getElement());
		onScreenShown();
		onAttach();
	}

	private void onScreenShown() {
		ServerGwtEndpoint service = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default");
		service.allComplete(new DefaultAsyncHandler<List<ItemValue<Server>>>() {

			@Override
			public void success(List<ItemValue<Server>> value) {
				GWT.log("Filling hosts grid with " + value.size() + " hosts.");
				grid.setValues(value);
				grid.selectAll(false);
			}
		});
	}

	private void deleteServers(List<ItemValue<Server>> listSelection, String confirm) {
		ServerGwtEndpoint service = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default");
		if (Window.confirm(confirm)) {
			List<ItemValue<Server>> toDelete = new ArrayList<>(listSelection);
			grid.clearSelectionModel();

			for (final ItemValue<Server> server : toDelete) {

				service.delete(server.uid, new DefaultAsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						Notification.get().reportInfo("Server " + server.value.name + " deleted");
						List<ItemValue<Server>> currentValues = grid.getValues();
						currentValues.remove(server);
						grid.setValues(currentValues);
					}
				});
			}
		}
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

}
