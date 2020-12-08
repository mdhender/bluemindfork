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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

import net.bluemind.ui.admin.client.forms.l10n.stringedit.StringEditConstants;

public class MultiStringEditContainer extends Composite implements HasChangeHandlers {

	private FlowPanel itemContainer;
	private Anchor addItem;
	private List<StringEditItem> items;
	private int minimumLength = 0;

	public static final StringEditConstants constants = GWT.create(StringEditConstants.class);

	@UiConstructor
	public MultiStringEditContainer() {
		FlowPanel container = new FlowPanel();
		initWidget(container);

		itemContainer = new FlowPanel();

		addItem = new Anchor(constants.add());
		addItem.addClickHandler(clickevent -> addItem(""));

		container.add(itemContainer);
		container.add(addItem);

		items = new ArrayList<>();
	}

	public void setMinimumLength(int length) {
		minimumLength = length;
	}

	private void addItem(String item) {
		final StringEditItem cp = new StringEditItem(item);
		itemContainer.add(cp);
		items.add(cp);
		if (minimumLength > 0) {
			cp.setVisibleLength(minimumLength);
		}
		cp.getTextBox().addChangeHandler(change -> fireChangeEvent());
		cp.getTrash().addClickHandler(clickevent -> {
			itemContainer.remove(cp);
			items.remove(cp);
			fireChangeEvent();
		});
		fireChangeEvent();
	}

	public Set<String> getValues() {
		Set<String> ret = new HashSet<>();
		for (StringEditItem item : items) {
			ret.add(item.getStringValue());
		}
		return ret;
	}

	public void setValues(Set<String> aliases) {
		resetScreen();
		for (String value : aliases) {
			addItem(value);
		}
	}

	public void setReadOnly(String value, Boolean readOnly) {
		for (StringEditItem item : items) {
			if (item.getStringValue().equals(value)) {
				item.setReadOnly(readOnly);
				break;
			}
		}
	}

	private void resetScreen() {
		for (int i = 0; i < itemContainer.getWidgetCount(); i++) {
			itemContainer.remove(i);
		}
		items.clear();
		fireChangeEvent();
	}

	@Override
	public HandlerRegistration addChangeHandler(ChangeHandler handler) {
		return addHandler(handler, ChangeEvent.getType());
	}

	private void fireChangeEvent() {
		DomEvent.fireNativeEvent(Document.get().createChangeEvent(), this);
	}

}
