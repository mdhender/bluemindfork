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

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

public class PrependedTextEdit extends AbstractTextEdit<String>implements ICommonEditor {

	public static interface Resources extends ClientBundle {

		@Source("PrependedTextEdit.css")
		Style editStyle();

	}

	public static interface Style extends CssResource {
		String textBox();

		String prependedText();
	}

	protected static final Resources res = GWT.create(Resources.class);
	protected Style s;
	protected FlowPanel container;
	protected Label prependedText;
	protected TextBox textBox;

	public PrependedTextEdit() {
	}

	@Override
	public void setStringValue(String v) {
		textBox.setValue(v);

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

	@Override
	public ITextEditor<String> createTextBox() {

		s = res.editStyle();
		s.ensureInjected();

		container = new FlowPanel();

		prependedText = new Label();
		prependedText.setStyleName(s.prependedText());
		container.add(prependedText);

		textBox = new TextBox();
		textBox.setStyleName(s.textBox());
		container.add(textBox);
		return new ITextEditor<String>() {

			@Override
			public ValueBoxEditor<String> asEditor() {
				return new ValueBoxEditor<String>(textBox) {

					@Override
					public void setValue(String value) {
						textBox.setValue(value);
					}

					@Override
					public String getValue() {
						return textBox.getValue();
					}

				};
			}

			@Override
			public Widget asWidget() {
				return container;
			}

			@Override
			public void setEnabled(boolean b) {
				textBox.setEnabled(b);
			}

		};
	}

	public void setMaxLength(int len) {
		textBox.setMaxLength(len);
	}

}
