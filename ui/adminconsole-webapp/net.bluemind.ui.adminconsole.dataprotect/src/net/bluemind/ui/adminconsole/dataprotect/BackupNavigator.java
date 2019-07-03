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

package net.bluemind.ui.adminconsole.dataprotect;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;

import net.bluemind.ui.adminconsole.base.ui.AppScreen;

public class BackupNavigator extends AppScreen {

	public BackupNavigator() {
		super(false);
		Button b = new Button("click click");
		b.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent ce) {
				Window.alert("yeah yeah");
				GWT.log("in the log");
			}
		});
		initWidget(b);
	}

}
