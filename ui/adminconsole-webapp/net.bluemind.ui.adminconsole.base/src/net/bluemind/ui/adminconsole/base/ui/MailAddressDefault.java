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
package net.bluemind.ui.adminconsole.base.ui;

import java.util.Set;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.ListBox;

public class MailAddressDefault extends ListBox {

	public MailAddressDefault() {
		super();
		getElement().setId("default-mail-address");

		addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				updateTitle();
			}
		});
	}

	public void updateDefaultAddressList(Set<String> mailAddresses, String defaultEmail) {
		String selectedItem = null;

		if (defaultEmail != null) {
			selectedItem = defaultEmail;
		} else if (getSelectedIndex() != -1) {
			selectedItem = getItemText(getSelectedIndex());
		}

		clear();

		for (String email : mailAddresses) {
			super.addItem(email);
		}

		if (selectedItem != null) {
			for (int i = 0; i < getItemCount(); i++) {
				if (getItemText(i).equals(selectedItem)) {
					setSelectedIndex(i);
					break;
				}
			}
		}

		updateTitle();
	}

	private void updateTitle() {
		if (getSelectedIndex() != -1) {
			setTitle(getItemText(getSelectedIndex()));
		} else {
			setTitle(null);
		}
	}
}
