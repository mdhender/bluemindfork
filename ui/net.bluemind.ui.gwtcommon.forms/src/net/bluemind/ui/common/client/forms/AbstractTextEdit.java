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

import java.util.HashSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

/**
 * Use this widget for string properties editing in crud forms
 * 
 * 
 */
public abstract class AbstractTextEdit<T> extends Composite implements IsEditor<ValueBoxEditor<T>>, ICommonEditor {

	public static interface Resources extends ClientBundle {
		@Source("Common.css")
		Style editStyle();
	}

	public static interface Style extends CssResource {
		String labelMandatory();

		String inputMandatory();
	}

	protected static final Resources res = GWT.create(Resources.class);
	protected Style s;
	protected ITextEditor<T> text;
	protected Label title;
	private boolean readOnly;
	protected HashSet<IFormChangeListener> changeListeners;

	public AbstractTextEdit() {
		s = res.editStyle();
		s.ensureInjected();

		TrPanel tr = new TrPanel();
		tr.setStyleName("setting");
		text = createTextBox();
		title = new Label();
		tr.add(title, "label");
		tr.add(text.asWidget(), "form");
		initWidget(tr);
	}

	public abstract ITextEditor<T> createTextBox();

	public String getTitleText() {
		return title.getText();
	}

	@Override
	public void setTitleText(String titleText) {
		title.setText(titleText);
	}

	public void setMandatory(boolean b) {
		if (b) {
			title.setText(title.getText() + " *");
			title.addStyleName(s.labelMandatory());
			text.asWidget().addStyleName(s.inputMandatory());
		}
	}

	@Override
	public ValueBoxEditor<T> asEditor() {
		return text.asEditor();
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		text.setEnabled(!readOnly);
	}

	public void setFocus(boolean b) {
		try {
			FocusWidget w = (FocusWidget) text.asWidget();
			w.setFocus(true);
		} catch (Throwable t) {
			GWT.log("focusing failed...", t);
			// catch class cast exception
		}
	}

	@Override
	public String getStringValue() {
		T value = asEditor().getValue();
		return value == null ? null : value.toString();
	}

	protected void dispatchChange() {
		for (IFormChangeListener listener : changeListeners) {
			listener.onChange(this);
		}
	}

	@Override
	public void addFormChangeListener(IFormChangeListener listener) {
		changeListeners.add(listener);
	}

	public void setId(String id) {
		text.asWidget().getElement().setId(id);

	}
}
