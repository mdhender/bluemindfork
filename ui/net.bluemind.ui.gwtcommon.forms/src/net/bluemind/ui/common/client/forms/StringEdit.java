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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.event.dom.client.HasKeyPressHandlers;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * Use this widget for string properties editing in crud forms
 * 
 * 
 */
public class StringEdit extends AbstractTextEdit<String>
		implements HasKeyPressHandlers, HasValueChangeHandlers<String> {

	private TextBox tb;

	@Override
	public ITextEditor<String> createTextBox() {
		tb = new TextBox();
		tb.addValueChangeHandler(evt -> ValueChangeEvent.fire(StringEdit.this, evt.getValue()));
		return new ITextEditor<String>() {

			@Override
			public Widget asWidget() {
				return tb.asWidget();
			}

			@Override
			public ValueBoxEditor<String> asEditor() {
				return tb.asEditor();
			}

			@Override
			public void setEnabled(boolean b) {
				tb.setEnabled(b);
			}

		};
	}

	public void setMaxLength(int len) {
		tb.setMaxLength(len);
	}

	public int getMaxLength() {
		return tb.getMaxLength();
	}

	public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
		return tb.addKeyUpHandler(handler);
	}

	@Override
	public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
		return tb.addKeyPressHandler(handler);
	}

	@Override
	public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<String> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	@Override
	public void setStringValue(String v) {
		if (v != null) {
			asEditor().setValue(v);
		}
	}

	public Map<String, Widget> getWidgetsMap() {
		Map<String, Widget> ret = new HashMap<String, Widget>();
		ret.put("label", title);
		ret.put("form", tb);
		return ret;
	}

	public void setDescriptionText(String s) {
		// TODO Auto-generated method stub

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

	@Override
	public void setId(String id) {
		tb.getElement().setId(id);

	}
}
