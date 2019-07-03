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
package net.bluemind.ui.adminconsole.directory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;

import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.MenuButton;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;

public class NewButton extends MenuButton {

	public static interface NewButtonConstants extends Constants {

		String user();

		String group();

		String resource();

		String mailshare();

		String addressBook();

		String calendar();

		String externalUser();
	}

	private ScreenShowRequest ssr;

	private static final NewButtonConstants constants = GWT.create(NewButtonConstants.class);

	public NewButton(String lbl, PopupOrientation popupOrientation) {
		super(lbl, popupOrientation);
		ssr = new ScreenShowRequest();
		createContent();
	}

	private void createContent() {
		FlexTable ft = new FlexTable();

		int idx = 0;
		if (inRole(BasicRoles.ROLE_MANAGE_USER)) {
			Anchor newUser = new Anchor(constants.user());
			newUser.getElement().setId("new-button-user");
			ft.setWidget(idx++, 0, newUser);
			newUser.addClickHandler(createUserPopup());
		}

		if (inRole(BasicRoles.ROLE_MANAGE_GROUP)) {
			Anchor newGroup = new Anchor(constants.group());
			newGroup.getElement().setId("new-button-group");
			ft.setWidget(idx++, 0, newGroup);
			newGroup.addClickHandler(createGroupPopup());
		}

		if (inRole(BasicRoles.ROLE_MANAGE_RESOURCE)) {
			Anchor newResource = new Anchor(constants.resource());

			newResource.getElement().setId("new-button-resource");
			ft.setWidget(idx++, 0, newResource);
			newResource.addClickHandler(createResourcePopup());
		}

		if (inRole(BasicRoles.ROLE_MANAGE_MAILSHARE)) {
			Anchor newMailShare = new Anchor(constants.mailshare());
			newMailShare.getElement().setId("new-button-mailshare");
			ft.setWidget(idx++, 0, newMailShare);
			newMailShare.addClickHandler(createMailsharePopup());
		}

		if (inRole(BasicRoles.ROLE_MANAGE_DOMAIN_CAL)) {
			Anchor newCalendar = new Anchor(constants.calendar());
			newCalendar.getElement().setId("new-button-calendar");
			ft.setWidget(idx++, 0, newCalendar);
			newCalendar.addClickHandler(createCalendarPopup());
		}

		if (inRole(BasicRoles.ROLE_MANAGE_EXTERNAL_USER)) {
			Anchor newExternalUser = new Anchor(constants.externalUser());
			newExternalUser.getElement().setId("new-button-externaluser");
			ft.setWidget(idx++, 0, newExternalUser);
			newExternalUser.addClickHandler(createExternalUserPopup());
		}

		if (inRole(BasicRoles.ROLE_MANAGE_DOMAIN_AB)) {
			Anchor newBook = new Anchor(constants.addressBook());
			newBook.getElement().setId("new-button-addressbook");
			ft.setWidget(idx++, 0, newBook);
			newBook.addClickHandler(createAddressBookPopup());
		}

		pp.add(ft);
		if (idx == 0) {
			setEnabled(false);
		}
	}

	private boolean inRole(String role) {
		return net.bluemind.ui.common.client.forms.Ajax.TOKEN.getRoles().contains(role)
				|| net.bluemind.ui.common.client.forms.Ajax.TOKEN.getRolesByOrgUnits().values().stream()
						.filter(e -> e.contains(role)).count() > 0;
	}

	private ClickHandler createMailsharePopup() {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().show("qcMailshare", ssr);
				setDown(false);
				pp.hide();
			}
		};
	}

	private ClickHandler createResourcePopup() {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().show("qcResource", ssr);
				setDown(false);
				pp.hide();
			}
		};
	}

	private ClickHandler createGroupPopup() {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().show("qcGroup", ssr);
				setDown(false);
				pp.hide();
			}
		};
	}

	private ClickHandler createUserPopup() {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().show("qcUser", ssr);
				setDown(false);
				pp.hide();
			}
		};
	}

	private ClickHandler createCalendarPopup() {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().show("qcCalendar", ssr);
				setDown(false);
				pp.hide();
			}
		};
	}

	private ClickHandler createAddressBookPopup() {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().show("qcBook", ssr);
				setDown(false);
				pp.hide();
			}
		};
	}

	private ClickHandler createExternalUserPopup() {
		return new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				Actions.get().show("qcExternalUser", ssr);
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
