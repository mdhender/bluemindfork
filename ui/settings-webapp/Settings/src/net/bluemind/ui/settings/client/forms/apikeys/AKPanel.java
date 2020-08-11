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
package net.bluemind.ui.settings.client.forms.apikeys;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.authentication.api.APIKey;
import net.bluemind.authentication.api.IAPIKeysAsync;
import net.bluemind.authentication.api.gwt.endpoint.APIKeysGwtEndpoint;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.icon.Trash;

public class AKPanel extends CompositeGwtWidgetElement {

	private final IAPIKeysAsync apiKeys = new APIKeysGwtEndpoint(Ajax.TOKEN.getSessionId());
	private static AKPanelUiBinder uiBinder = GWT.create(AKPanelUiBinder.class);

	interface AKPanelUiBinder extends UiBinder<HTMLPanel, AKPanel> {
	}

	public static interface Resources extends ClientBundle {
		@Source("AKPanel.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {

		String container();

		String item();

		String sid();

		String dn();
	}

	private static final Resources res = GWT.create(Resources.class);
	private final Style s;

	@UiField
	Button createBtn;

	@UiField
	TextBox dn;

	@UiHandler("dn")
	public void onKeyUpEvent(KeyUpEvent keyPress) {
		if (keyPress.getNativeKeyCode() == KeyCodes.KEY_ENTER && !dn.getValue().isEmpty()) {
			genApiKey();
		}
	}

	@UiField
	FlexTable table;

	public AKPanel() {
		s = res.editStyle();
		s.ensureInjected();

		FlowPanel panel = new FlowPanel();
		HTMLPanel form = uiBinder.createAndBindUi(this);
		panel.add(form);

		initWidget(panel);

		table.setStyleName(s.container());

		createBtn.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				genApiKey();
			}
		});

		dn.getElement().setAttribute("placeholder", AKConstants.INST.dn());

		getApiKeys();

	}

	private void getApiKeys() {
		table.clear();
		apiKeys.list(new AsyncHandler<List<APIKey>>() {

			@Override
			public void success(List<APIKey> result) {
				for (APIKey k : result) {
					addEntry(k);
				}
			}

			@Override
			public void failure(Throwable caught) {
				Notification.get().reportError(caught);
			}
		});
	}

	private void addEntry(final APIKey k) {
		Trash trash = new Trash();
		trash.addClickHandler(event -> {
			if (Window.confirm(AKConstants.INST.confirmDelete(k.displayName))) {
				Cell c = table.getCellForEvent(event);
				table.removeRow(c.getRowIndex());
				revokeApiKey(k.sid);
			}
		});
		int row = table.getRowCount();
		int i = 0;

		FlowPanel fp = new FlowPanel();
		Label sid = new Label(k.sid);
		sid.setStyleName(s.sid());
		fp.add(sid);

		Label dn = new Label(k.displayName);
		dn.setStyleName(s.dn());
		fp.add(dn);

		table.setWidget(row, i++, fp);
		table.setWidget(row, i++, trash);
		table.getRowFormatter().setStyleName(row, s.item());

	}

	private void revokeApiKey(String sid) {
		apiKeys.delete(sid, new AsyncHandler<Void>() {

			@Override
			public void failure(Throwable caught) {
				Notification.get().reportError(caught);
			}

			@Override
			public void success(Void result) {
				Notification.get().reportInfo(AKConstants.INST.deleteOk());
			}

		});

	}

	private void genApiKey() {
		if (dn.getValue().isEmpty()) {
			Window.alert(AKConstants.INST.dnIsEmpty());
		} else {
			apiKeys.create(dn.getValue(), new AsyncHandler<APIKey>() {
				@Override
				public void success(APIKey result) {
					dn.setValue(null);
					addEntry(result);
					Notification.get().reportInfo(AKConstants.INST.insertOk());
				}

				@Override
				public void failure(Throwable caught) {

					Notification.get().reportError(caught);
				}
			});
		}
	}

	public native static ScreenElement create()
	/*-{
		return {
			'id' : 'apiKeys',
			'type' : 'bm.settings.ApiKeysEditor'
		};
	}-*/;

	public static void registerType() {
		GwtWidgetElement.register("bm.settings.ApiKeysEditor",
				new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

					@Override
					public IGwtWidgetElement create(WidgetElement el) {
						return new AKPanel();
					}
				});
	}

}
