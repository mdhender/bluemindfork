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
package net.bluemind.ui.adminconsole.system.subscription;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

public class SubContact extends Composite {
	
	private Label emailIcon = new Label();
	private Label email = new Label();
	private Label removeIcon = new Label();

	public SubContact(String mailAddress) {
		email.setText(mailAddress);
		email.setVisible(true);
		
		emailIcon.setStyleName("fa fa-envelope");
		removeIcon.setStyleName("fa fa-lg fa-minus-square-o");
		
		HorizontalPanel panel = new HorizontalPanel();

		panel.add(emailIcon);
		panel.add(email);
		panel.add(removeIcon);
		
		initWidget(panel);
	}

	public Label getIcon() {
		return removeIcon;
	}
}
