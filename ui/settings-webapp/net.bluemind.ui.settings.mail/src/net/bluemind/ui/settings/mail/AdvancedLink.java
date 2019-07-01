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
package net.bluemind.ui.settings.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;

public class AdvancedLink extends CompositeGwtWidgetElement {

	public static final String TYPE = "bm.settings.AdvancedMailLink";
	private final MailMessages messages = GWT.create(MailMessages.class);
	private FlexTable table;

	public AdvancedLink() {
		table = new FlexTable();
		table.setStyleName("formContainer");
		initWidget(table);

		int i = 0;
		ct(i++, messages.advanced(), new Anchor(messages.advancedAnchor(), "/webmail/?_task=settings&_action="));
	}

	private void ct(int i, String label, Widget w) {
		table.setWidget(i, 0, new Label(label));
		table.setWidget(i, 1, w);
		table.getRowFormatter().setStyleName(i, "setting");
		table.getCellFormatter().setStyleName(i, 0, "label");
		table.getCellFormatter().setStyleName(i, 1, "form");

	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement we) {
				return new AdvancedLink();
			}
		});

	}
}
