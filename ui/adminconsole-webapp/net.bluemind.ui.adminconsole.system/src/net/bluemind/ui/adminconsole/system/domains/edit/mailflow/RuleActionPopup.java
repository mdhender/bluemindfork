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
package net.bluemind.ui.adminconsole.system.domains.edit.mailflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;

public class RuleActionPopup extends PopupPanel {

	private final Map<Integer, String> mappings;

	public RuleActionPopup(List<String> identifiers, Consumer<String> consumer) {
		super(true);
		mappings = new HashMap<>();
		ListBox lb = new ListBox();
		int index = 0;
		for (String identifier : identifiers) {
			lb.addItem(RuleTexts.resolve(identifier));
			mappings.put(index++, identifier);
		}
		lb.setVisibleItemCount(identifiers.size());
		lb.addClickHandler(h -> {
			consumer.accept(mappings.get(lb.getSelectedIndex()));
			setVisible(false);
		});
		setWidget(lb);
	}

}
