/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)..
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

/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)..
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

/**
 * Use this widget for boolean properties editing in crud forms
 * 
 * 
 */
public class BooleanEdit extends Composite
		implements IsEditor<LeafValueEditor<Boolean>>, HasValue<Boolean>, ICommonEditor {

	public static interface Resources extends ClientBundle {

		@Source("StringEdit.css")
		Style stringEditStyle();

	}

	public static interface Style extends CssResource {

		String flowPanel();

		String textInput();

		String mandatory();

		String inputTitle();

		String vaTop();

	}

	private static final Resources RES = GWT.create(Resources.class);

	private CheckBox check;
	protected Label title;

	private boolean readOnly;

	private Style s;

	private TrPanel fp;

	private String propertyName;

	public BooleanEdit() {
		s = RES.stringEditStyle();
		s.ensureInjected();
		fp = new TrPanel();
		fp.addStyleName(s.flowPanel());
		title = new Label("");
		title.addStyleName(s.inputTitle());
		fp.add(title);
		check = new CheckBox();
		fp.add(check);
		initWidget(fp);
	}

	public BooleanEdit(String titleText) {
		this();
		setTitleText(titleText);
	}

	@Override
	public void setId(String id) {
		check.getElement().setId(id);
	}

	public String getTitleText() {
		return check.getText();
	}

	@Override
	public void setTitleText(String titleText) {
		check.setText(titleText);
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		check.setEnabled(!readOnly);
	}

	@Override
	public LeafValueEditor<Boolean> asEditor() {
		return check.asEditor();
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Boolean> handler) {
		return check.addValueChangeHandler(handler);
	}

	@Override
	public Boolean getValue() {
		return check.getValue();
	}

	@Override
	public void setValue(Boolean value) {
		check.setValue(value);
	}

	@Override
	public void setValue(Boolean value, boolean fireEvents) {
		check.setValue(value, fireEvents);
	}

	@Override
	public String getStringValue() {
		return getValue() == null ? null : getValue().toString();
	}

	@Override
	public void setStringValue(String v) {
		if (v != null) {
			asEditor().setValue(Boolean.parseBoolean(v));
		}
	}

	@Override
	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
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
	public void addFormChangeListener(IFormChangeListener listener) {
	}

}
