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
package net.bluemind.ui.settings.client.forms;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.common.client.forms.CommonForm;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

public class PurgeStorage extends CommonForm implements ICommonEditor {

	private Button button;
	private Storage storage;

	public PurgeStorage() {
		super();

		button = new Button();
		button.setText(PurgeStorageConstants.INST.buttonLabel());

		button.addStyleName("button");
		button.addStyleName("dangerHighVoltage");
		button.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				callJs(new AsyncHandler<Object>() {

					@Override
					public void success(Object value) {
						Notification.get().reportInfo(PurgeStorageConstants.INST.notif());
					}

					@Override
					public void failure(Throwable e) {
						Notification.get().reportError("error during local data purge");
					}
				});

			}

		});

		FlowPanel fp = new FlowPanel();
		Label sectionTitle = new Label(PurgeStorageConstants.INST.label());
		sectionTitle.setStyleName("sectionTitle");
		fp.add(sectionTitle);

		FlexTable table = new FlexTable();
		table.setWidget(0, 0, new Label(PurgeStorageConstants.INST.warningMsg()));
		table.setWidget(1, 0, button);

		fp.add(table);

		form = fp;
	}

	@Override
	public void setTitleText(String s) {
		label.setText(s);
	}

	@Override
	public String getStringValue() {
		return null;
	}

	@Override
	public void setStringValue(String v) {
	}

	@Override
	public void setPropertyName(String string) {
	}

	@Override
	public Widget asWidget() {
		return null;
	}

	@Override
	public String getPropertyName() {
		return null;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
	}

	private native <T> void callJs(AsyncHandler<Object> handler)
	/*-{
    $wnd['DatabaseUtils']['reset'](function() {
      handler.@net.bluemind.core.api.AsyncHandler::success(Ljava/lang/Object;)(null);
    }, function(e) {
      handler.@net.bluemind.core.api.AsyncHandler::failure(Ljava/lang/Throwable;)(e);
    });
	}-*/;
}
