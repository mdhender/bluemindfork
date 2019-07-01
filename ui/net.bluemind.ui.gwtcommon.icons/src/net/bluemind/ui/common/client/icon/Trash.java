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
package net.bluemind.ui.common.client.icon;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;

public class Trash extends FocusPanel {

	private FocusPanel lidPanel;
	private FocusPanel canPanel;

	public Trash() {
		setTitle(TrashConstants.INST.removeFromList());

		FlowPanel container = new FlowPanel();
		container.setStyleName("goog-inline-block goog-trash-button");

		lidPanel = new FocusPanel();
		lidPanel.setStyleName("goog-trash-button-lid fa fa-lg fa-trash-o");

		canPanel = new FocusPanel();
		canPanel.setStyleName("goog-trash-button-can fa fa-lg fa-trash-o");

		container.add(lidPanel);
		container.add(canPanel);
		add(container);
		container.getElement().getStyle().setLineHeight(15, Unit.PX);
		container.getElement().getStyle().setHeight(15, Unit.PX);
	}

	public void setId(String id) {
		getElement().setId(id + "trash-can");
	}
}
