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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

import net.bluemind.ui.admin.client.forms.l10n.stringedit.StringEditConstants;

public class MultiStringEditContainer extends Composite {

	private FlowPanel itemContainer;
	private Anchor addItem;
	private List<StringEditItem> items;

	public static final StringEditConstants constants = GWT.create(StringEditConstants.class);

	@UiConstructor
	public MultiStringEditContainer() {
		FlowPanel container = new FlowPanel();
		initWidget(container);

		itemContainer = new FlowPanel();

		addItem = new Anchor(constants.add());
		addItem.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				addItem("");
			}
		});

		container.add(itemContainer);
		container.add(addItem);

		items = new ArrayList<>();
	}

	private void addItem(String item) {
		final StringEditItem cp = new StringEditItem(item);

		itemContainer.add(cp);
		items.add(cp);

		cp.getTrash().addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				itemContainer.remove(cp);
				items.remove(cp);
			}
		});

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

	private void resetScreen() {
		for (int i = 0; i < itemContainer.getWidgetCount(); i++) {
			itemContainer.remove(i);
		}
		items.clear();
	}

}
