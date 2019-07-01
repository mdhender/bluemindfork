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
package net.bluemind.ui.common.client.forms;

import java.util.List;
import java.util.Map;

import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;

public class ListBoxEditor extends Composite implements IsEditor<LeafValueEditor<String>> {

	private ListBox listBox = new ListBox();
	private String value;
	private LeafValueEditor<String> editor = new LeafValueEditor<String>() {

		@Override
		public void setValue(String value) {
			ListBoxEditor.this.value = value;
			updateValue();
		}

		@Override
		public String getValue() {
			return value;
		}

	};

	public ListBoxEditor() {

		listBox.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				value = listBox.getSelectedValue();
			}
		});
	}

	public void initValues(List<Map.Entry<String, String>> textAndValues) {
		listBox.clear();
		for (Map.Entry<String, String> v : textAndValues) {
			listBox.addItem(v.getKey(), v.getValue());
		}
		updateValue();
	}

	private void updateValue() {
		for (int i = 0; i < listBox.getItemCount(); i++) {
			String itemValue = listBox.getValue(i);
			if (itemValue == value || itemValue.equals(value)) {
				listBox.setSelectedIndex(i);
				break;
			}
		}
	}

	@Override
	public LeafValueEditor<String> asEditor() {
		return editor;
	}

}
