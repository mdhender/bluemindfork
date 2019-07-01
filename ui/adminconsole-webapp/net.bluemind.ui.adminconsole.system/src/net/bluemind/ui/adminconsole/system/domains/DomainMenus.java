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
package net.bluemind.ui.adminconsole.system.domains;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainMenuConstants;
import net.bluemind.ui.adminconsole.system.l10n.SystemMenuConstants;

public class DomainMenus {

	public static MenuContribution getContribution() {
		DomainMenuConstants v = GWT.create(DomainMenuConstants.class);
		SystemMenuConstants v2 = GWT.create(SystemMenuConstants.class);
		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();

		screens.push(Contributed.create("system", Screen.create("editDomain", v.editDomain(), null, false)));
		screens.push(Contributed.create("domains",
				Screen.create("qcDomain", v2.createDomain(), BasicRoles.ROLE_MANAGE_DOMAIN, false)));
		screens.push(Contributed.create("domains", Screen.create("domainsManager", v2.domainsManager(), true)
				.withRoles(BasicRoles.ROLE_MANAGE_DOMAIN, BasicRoles.ROLE_ADMIN)));

		sections.push(Contributed.create("system", Section.createWithPriority("domains", v2.domains(), 90)));
		return MenuContribution.create(sections, screens);
	}
}
