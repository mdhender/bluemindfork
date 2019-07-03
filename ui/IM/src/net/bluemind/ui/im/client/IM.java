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
package net.bluemind.ui.im.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;

import net.bluemind.gwtconsoleapp.base.lifecycle.GwtAppLifeCycle;
import net.bluemind.gwtconsoleapp.base.lifecycle.ILifeCycle;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.im.client.push.Push;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class IM implements EntryPoint {

	public void onModuleLoad() {

		GwtAppLifeCycle.registerLifeCycle("net.bluemind.ui.instantmessaging", new ILifeCycle() {

			@Override
			public void start() {
				startUp();
			}
		});
	}

	protected void startUp() {

		if (Ajax.TOKEN.getRoles().contains(BasicRoles.ROLE_IM)) {
			RootLayoutPanel rlp = RootLayoutPanel.get();
			rlp.clear();
			RootScreen rs = new RootScreen();
			rlp.add(rs);
			Push.setup();
		} else {
			RootLayoutPanel rlp = RootLayoutPanel.get();
			rlp.clear();
			Label l = new Label("Forbidden");
			rlp.add(l);
		}
	}

}
