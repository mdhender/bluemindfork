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
package net.bluemind.ui.adminconsole.security;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributorUnwrapped;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.security.l10n.SecurityMenuConstants;

public class SecurityMenusContributor implements MenuContributorUnwrapped {

	@Override
	public MenuContribution contribution() {

		SecurityMenuConstants v = GWT.create(SecurityMenuConstants.class);

		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();
		JsArray<Section> ssections = JsArray.createArray().cast();
		ssections.push(Section.createVerySimple("ssl", v.certs(), null));
		ssections.push(Section.createVerySimple("iptables", v.firewall(), null));
		sections.push(Contributed.create(null, Section.create("security", v.security(), 97, "fa-shield",
				JsArray.createArray().<JsArray<Screen>> cast(), ssections)));

		screens.push(Contributed.create("ssl", Screen.create("proxyCert", v.editcerts(), //
				BasicRoles.ROLE_MANAGE_SYSTEM_CONF, true)));
		screens.push(Contributed.create("iptables", Screen.create("iptablesRules", v.editfirewall(), //
				BasicRoles.ROLE_MANAGE_SYSTEM_CONF, true)));
		return MenuContribution.create(sections, screens);
	}
}
