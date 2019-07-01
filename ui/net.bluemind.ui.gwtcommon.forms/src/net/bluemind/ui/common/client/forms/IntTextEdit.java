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

import java.util.Map;

import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.user.client.ui.IntegerBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Use this widget for string properties editing in crud forms
 * 
 * 
 */
public class IntTextEdit extends AbstractTextEdit<Integer> {

	private IntegerBox tr;

	@Override
	public ITextEditor<Integer> createTextBox() {
		this.tr = new IntegerBox();
		return new ITextEditor<Integer>() {

			@Override
			public Widget asWidget() {
				return tr.asWidget();
			}

			@Override
			public ValueBoxEditor<Integer> asEditor() {
				return tr.asEditor();
			}

			@Override
			public void setEnabled(boolean b) {
				tr.setEnabled(b);
			}

		};
	}

	@Override
	public void setStringValue(String v) {
		if (v != null) {
			asEditor().setValue(Integer.parseInt(v));
		}
	}

	@Override
	public void setDescriptionText(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, Widget> getWidgetsMap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPropertyName(String string) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getPropertyName() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setMaxLength(int maxLength) {
		tr.setMaxLength(maxLength);
	}

	public int getMaxLength() {
		return tr.getMaxLength();
	}
}
