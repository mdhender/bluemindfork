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
package net.bluemind.ui.admin.client.forms;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.ui.common.client.icon.Trash;

public class StringEditItem extends Composite {

	private TextBox item;
	private Trash trash;

	public StringEditItem(String value) {
		FlexTable ft = new FlexTable();
		initWidget(ft);

		item = new TextBox();
		item.setText(value);
		ft.setWidget(0, 0, item);

		trash = new Trash();
		ft.setWidget(0, 2, trash);
		ft.getFlexCellFormatter().setRowSpan(0, 2, 2);
	}

	public void setVisibleLength(int length) {
		item.setVisibleLength(length);
	}

	public Boolean isReadOnly() {
		return item.isEnabled();
	}

	public void setReadOnly(Boolean readOnly) {
		trash.setVisible(!readOnly);
		item.setReadOnly(readOnly);
		item.setEnabled(readOnly);
	}

	public Trash getTrash() {
		return trash;
	}

	public TextBox getTextBox() {
		return item;
	}

	public String getStringValue() {
		return item.getText();
	}
}
