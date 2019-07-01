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

import java.util.Map;

import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.common.client.forms.ITextEditor;

/**
 * Use this widget for string properties editing in crud forms
 * 
 * 
 */
public class TextEdit extends AbstractACTextEdit<String> {

	private TextArea tr;

	@Override
	public ITextEditor<String> createTextBox() {
		this.tr = new TextArea();
		tr.setVisibleLines(4);
		return new ITextEditor<String>() {

			@Override
			public Widget asWidget() {
				return tr.asWidget();
			}

			@Override
			public ValueBoxEditor<String> asEditor() {
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
			asEditor().setValue(v);
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

}
