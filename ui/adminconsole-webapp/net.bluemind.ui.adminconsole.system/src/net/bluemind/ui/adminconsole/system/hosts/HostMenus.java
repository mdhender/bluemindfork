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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.system.l10n.SystemMenuConstants;

public class HostMenus {

	public static MenuContribution getContribution() {
		SystemMenuConstants v2 = GWT.create(SystemMenuConstants.class);
		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();

		screens.push(Contributed.create("sys1",
				Screen.create("hosts", v2.applicationServers(), BasicRoles.ROLE_MANAGE_SERVER, true)));
		// FIXME should not be here
		screens.push(Contributed.create("sys1",
				Screen.create("systemConf", v2.systemConfiguration(), BasicRoles.ROLE_MANAGE_SYSTEM_CONF, false)));
		screens.push(Contributed.create("sys1", Screen.create("subscription", v2.subscriptionInstallation(),
				BasicRoles.ROLE_MANAGE_SUBSCRIPTION, false)));

		screens.push(Contributed.create("system", Screen.create("editHost", "Edit Host", null, false)));
		Section servers = Section.createWithPriority("sys1", v2.servers(), 200);
		JsArrayString roles = JsArrayString.createArray().cast();
		roles.push(BasicRoles.ROLE_MANAGE_SERVER);
		sections.push(Contributed.create("system", servers));
		return MenuContribution.create(sections, screens);
	}
}
