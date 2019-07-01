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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;

public class CommonForm implements ICommonEditor {
	protected Label label;
	protected Label description;
	protected Widget form;
	protected Map<String, Widget> widgets;
	private Set<IFormChangeListener> changeListeners;

	public CommonForm() {
		label = new Label();
		description = new Label();
		widgets = new HashMap<String, Widget>();
		changeListeners = new HashSet<IFormChangeListener>();
	}

	public Label getDescription() {
		return description;
	}

	public void setDescription(Label description) {
		this.description = description;
	}

	public void setDescriptionText(String desc) {
		description.setText(desc);
	}

	public Label getLabel() {
		return label;
	}

	public void setLabel(Label label) {
		this.label = label;
	}

	public Map<String, Widget> getWidgets() {
		return widgets;
	}

	public void setWidgets(Map<String, Widget> widgets) {
		this.widgets = widgets;
	}

	public Map<String, Widget> getWidgetsMap() {
		Map<String, Widget> ret = new HashMap<String, Widget>();
		ret.put("label", label);
		ret.put("form", form);
		ret.put("description", description);
		return ret;
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

	@Override
	public void setTitleText(String s) {
	}

	@Override
	public String getStringValue() {
		return null;
	}

	@Override
	public void setStringValue(String v) {
	}

	@Override
	public void setPropertyName(String string) {
	}

	@Override
	public Widget asWidget() {
		return form;
	}

	@Override
	public String getPropertyName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setReadOnly(boolean readOnly) {

	}

	@Override
	public void setId(String id) {
		// TODO Auto-generated method stub

	}
}
