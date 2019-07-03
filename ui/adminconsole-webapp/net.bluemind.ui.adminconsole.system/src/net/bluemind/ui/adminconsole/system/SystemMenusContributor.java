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
package net.bluemind.ui.adminconsole.system;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.ContributorUtil;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributorUnwrapped;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.ui.adminconsole.system.domains.DomainMenus;
import net.bluemind.ui.adminconsole.system.hosts.HostMenus;
import net.bluemind.ui.adminconsole.system.l10n.SystemMenuConstants;
import net.bluemind.ui.adminconsole.system.maintenance.MaintenanceMenus;

public class SystemMenusContributor implements MenuContributorUnwrapped {

	@Override
	public MenuContribution contribution() {

		SystemMenuConstants v2 = GWT.create(SystemMenuConstants.class);
		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();

		sections.push(Contributed.create(null, Section.create("system", v2.systemManagement(), 98, "fa-wrench",
				JsArray.createArray().<JsArray<Screen>> cast(), JsArray.createArray().<JsArray<Section>> cast())));

		MenuContribution systemContrib = MenuContribution.create(sections, screens);
		return ContributorUtil.mergeMenuContributions(systemContrib, DomainMenus.getContribution(),
				HostMenus.getContribution(), MaintenanceMenus.getContribution());
	}

}
