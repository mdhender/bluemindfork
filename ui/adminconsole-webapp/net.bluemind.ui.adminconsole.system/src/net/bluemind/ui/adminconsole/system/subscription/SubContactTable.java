/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.ui.adminconsole.system.subscription;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.VerticalAlign;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.system.api.gwt.endpoint.InstallationEndpointPromise;
import net.bluemind.system.api.gwt.endpoint.InstallationGwtEndpoint;
import net.bluemind.ui.adminconsole.system.subscription.l10n.SubscriptionConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class SubContactTable extends Composite {

	private VerticalPanel panel = new VerticalPanel();
	private HorizontalPanel addContactPanel;

	private TextBox addContact = new TextBox();
	private Label addContactIcon;
	private List<String> contacts = new ArrayList<>();
	
	private Label aboutSubContacts;
	private HTML noSubContacts;

	public static interface MailAddressTableConstants extends Constants {
		String defaultEmail();
	}

	@UiConstructor
	public SubContactTable() {
		addContact.getElement().setPropertyString("placeholder", SubscriptionConstants.INST.addContact());
		initWidget(panel);
		initPanel();
	}

	public void setInformationsPanels(Label aboutSubContacts, HTML noSubContacts) {
		this.aboutSubContacts = aboutSubContacts;
		this.noSubContacts = noSubContacts;
	}
	
	public boolean hasSubContact() {
		return !contacts.isEmpty();
	}
	
	private void initPanel() {
		addContactIcon = new Label();
		addContactIcon.setStyleName("fa fa-lg fa-plus-square-o");
		addContactIcon.getElement().getStyle().setVerticalAlign(VerticalAlign.MIDDLE);
		
		addContactIcon.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				addContact();
			}
		});
		addContact.addKeyDownHandler(addContactEnterKey());

		setAddContactPanel();
	}
	
	private void setAddContactPanel() {
		addContactPanel = new HorizontalPanel();
		addContactPanel.add(addContact);
		addContactPanel.add(addContactIcon);
		
		panel.add(addContactPanel);
	}
	
	private void removeAddContactPanel() {
		panel.remove(addContactPanel);
	}

	public void reset() {
		panel.clear();
		contacts.clear();
	}
	
	void display(JsArrayString emails) {
		reset();
		
		for (int i = 0; i < emails.length(); i++) {
			String email = emails.get(i);
			setNewEmail(email);
		}
		
		initPanel();
	}
	
	private SubContact setNewEmail(String email) {
		SubContact ma = new SubContact(email);
		ma.getIcon().addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				removeContact(email, ma);
			}
		});
		if (contacts.isEmpty()) {
			noSubContacts.setVisible(false);
			aboutSubContacts.setVisible(true);
		}
		contacts.add(email);
		panel.add(ma);
		return ma;
	}
	
	private void addContact() {
		String email = addContact.getText();
		if (email != null && !email.isEmpty() && !contacts.contains(email)) {
			removeAddContactPanel();
			SubContact sc = setNewEmail(email);
			new InstallationEndpointPromise(new InstallationGwtEndpoint(Ajax.TOKEN.getSessionId()))
					.setSubscriptionContacts(contacts).exceptionally(t -> {
						Notification.get()
								.reportError("New contact not added, server failed : \"" + t.getMessage() + "\"");
						removeContact(email, sc);
						return null;
					});
			setAddContactPanel();
			addContact.setText("");
		}
	}
	
	private void removeContact(String email, SubContact ma) {
		panel.remove(ma);
		contacts.remove(email);
		if (contacts.isEmpty()) {
			noSubContacts.setVisible(true);
			aboutSubContacts.setVisible(false);
		}
		new InstallationEndpointPromise(new InstallationGwtEndpoint(Ajax.TOKEN.getSessionId()))
				.setSubscriptionContacts(contacts);
	}
	
	private KeyDownHandler addContactEnterKey() {
		return new KeyDownHandler() {
			
			@Override
			public void onKeyDown(KeyDownEvent event) {
				if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
					addContact();
				}
			}
		};
	}

}
