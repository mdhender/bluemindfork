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
package net.bluemind.ui.settings.client.myaccount;

import java.util.Map;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;
import net.bluemind.ui.settings.client.forms.HTML5Notifications;
import net.bluemind.ui.settings.client.forms.PurgeStorage;

public class MyAccountAdvancedPartWidget extends CompositeGwtWidgetElement {
	public static final String TYPE = "bm.settings.Advanced";
	private FlexTable table;

	public MyAccountAdvancedPartWidget() {
		table = new FlexTable();
		table.setStyleName("formContainer");
		initWidget(table);

		int i = 0;
		ICommonEditor purgeStorage = new PurgeStorage();
		Map<String, Widget> purgeStorageWidgets = purgeStorage.getWidgetsMap();

		table.setWidget(i, 0, purgeStorageWidgets.get("form"));
		table.getFlexCellFormatter().setColSpan(i, 0, 3);

		i++;
		ICommonEditor html5notif = new HTML5Notifications();
		Map<String, Widget> html5notifWidgets = html5notif.getWidgetsMap();

		table.setWidget(i, 0, html5notifWidgets.get("form"));
		table.getFlexCellFormatter().setColSpan(i, 0, 3);

	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new MyAccountAdvancedPartWidget();
			}
		});
	}
}