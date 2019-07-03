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
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.ui.common.client.forms.CommonForm;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

public class HTML5Notifications extends CommonForm implements ICommonEditor {
	private Button button;

	public HTML5Notifications() {
		super();
		button = new Button();
		button.setText(HTML5NotificationsConstants.INST.button());
		button.addStyleName("button primary");
		boolean available = isHTML5NotificationsAvailable();
		if (available) {
			button.addClickHandler(new ClickHandler() {

				@Override
				public void onClick(ClickEvent event) {
					HTML5NotificationsRequest();
				}
			});
		}

		FlowPanel fp = new FlowPanel();
		Label sectionTitle = new Label(HTML5NotificationsConstants.INST.label());
		sectionTitle.setStyleName("sectionTitle");
		fp.add(sectionTitle);

		FlexTable table = new FlexTable();
		if (isHTML5NotificationsGranted()) {
			table.setWidget(0, 0, new Label(HTML5NotificationsConstants.INST.granted()));
		} else {
			if (available) {
				table.setWidget(0, 0, button);
			} else {
				table.setWidget(0, 0, new Label(HTML5NotificationsConstants.INST.unavailable()));
			}
		}
		fp.add(table);

		form = fp;
	}

	@Override
	public void setTitleText(String s) {
		label.setText(s);
	}

	public native boolean isHTML5NotificationsAvailable() /*-{
															return ("Notification" in $wnd);
															}-*/;

	public native void HTML5NotificationsRequest() /*-{
													Notification.requestPermission(function() {
													});
													}-*/;

	public native boolean isHTML5NotificationsGranted() /*-{
														try {
														return Notification.permission == "granted";
														} catch (e) {
														return false;
														}
														}-*/;
}
