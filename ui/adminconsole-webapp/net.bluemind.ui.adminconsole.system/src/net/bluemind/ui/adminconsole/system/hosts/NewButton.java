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
package net.bluemind.ui.adminconsole.system.hosts;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;

import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.MenuButton;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.adminconsole.system.hosts.l10n.HostConstants;

public class NewButton extends MenuButton {

	private ScreenShowRequest ssr;

	public NewButton(String lbl, PopupOrientation popupOrientation) {
		super(lbl, popupOrientation);
		ssr = new ScreenShowRequest();
		createContent();
	}

	private void createContent() {
		FlexTable ft = new FlexTable();

		int idx = 0;
		Anchor newUser = new Anchor(HostConstants.INST.host());
		newUser.getElement().setId("new-button-host");
		ft.setWidget(idx++, 0, newUser);
		newUser.addClickHandler(createHostLink());

		pp.add(ft);

	}

	private ClickHandler createHostLink() {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().show("qcHost", ssr);
				setDown(false);
				pp.hide();
			}
		};
	}

	public ScreenShowRequest getSsr() {
		return ssr;
	}

	public void setSsr(ScreenShowRequest ssr) {
		this.ssr = ssr;
	}

}
