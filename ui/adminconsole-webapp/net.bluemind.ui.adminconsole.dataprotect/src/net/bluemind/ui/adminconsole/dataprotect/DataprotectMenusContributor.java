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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributorUnwrapped;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.dataprotect.l10n.DPMenuConstants;

public class DataprotectMenusContributor implements MenuContributorUnwrapped {

	@Override
	public MenuContribution contribution() {

		DPMenuConstants v = GWT.create(DPMenuConstants.class);
		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();

		JsArray<Screen> sscreens = JsArray.createArray().cast();
		sscreens.push(Screen.create("dpGenBrowser", v.genBrowser(), null, true));

		JsArray<Screen> ssscreens = JsArray.createArray().cast();
		ssscreens.push(Screen.create("dpPolicy", v.configuration(), BasicRoles.ROLE_DATAPROTECT, true));

		JsArray<Section> ssections = JsArray.createArray().cast();
		ssections.push(Section.createSimple("dpSettings", v.settings(), BasicRoles.ROLE_DATAPROTECT, ssscreens));

		ssscreens = JsArray.createArray().cast();
		ssscreens.push(Screen.create("dpNavigator", v.navigator(), true)
				.withRoles(BasicRoles.ROLE_DATAPROTECT, BasicRoles.ROLE_MANAGE_RESTORE)
				.withOURoles(BasicRoles.ROLE_MANAGE_RESTORE));

		ssections.push(Section.createSimple("dataProtect", v.protectedData(), null, ssscreens));

		sections.push(Contributed.create(null,
				Section.create("backup", v.dataprotect(), 96, "fa-hdd-o", sscreens, ssections)));
		return MenuContribution.create(sections, screens);
	}
}
