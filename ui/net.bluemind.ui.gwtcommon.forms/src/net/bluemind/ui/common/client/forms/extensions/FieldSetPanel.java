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
package net.bluemind.ui.common.client.forms.extensions;

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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

@SuppressWarnings("deprecation")
public class FieldSetPanel extends ComplexPanel {

	private Element table;
	private Element legend;
	private FlowPanel container;
	private Label label;

	public FieldSetPanel() {
		table = DOM.createElement("table");
		table.setClassName("formContainer");
		container = new FlowPanel();
		container.getElement().appendChild(table);
		setElement(container.getElement());
	}

	public void setName(String name) {
		if (name != null && !name.isEmpty()) {
			label = new Label(name);
			label.setStyleName("sectionTitle");
			container.insert(label, 0);
		}
	}

	public String getName() {
		return legend != null ? legend.getInnerText() : null;
	}

	@Override
	public void add(Widget child) {
		super.add(child, table);
	}

}
