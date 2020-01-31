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
package net.bluemind.ui.adminconsole.system.maintenance;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.system.l10n.SystemMenuConstants;

public class MaintenanceMenus {

	public static MenuContribution getContribution() {
		SystemMenuConstants v2 = GWT.create(SystemMenuConstants.class);
		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();

		screens.push(Contributed.create("maintenance",
				Screen.create("updateBluemind", v2.updateBluemind(), BasicRoles.ROLE_SYSTEM_MANAGER, false)));
		screens.push(Contributed.create("maintenance",
				Screen.create("emailMaintenance", v2.maintainEmails(), BasicRoles.ROLE_SYSTEM_MANAGER, false)));
		screens.push(Contributed.create("maintenance",
				Screen.create("indexMaintenance", v2.reindexing(), BasicRoles.ROLE_SYSTEM_MANAGER, true)));

		sections.push(Contributed.create("system", Section.createWithPriority("maintenance", v2.maintenance(), 50)));

		return MenuContribution.create(sections, screens);
	}
}
