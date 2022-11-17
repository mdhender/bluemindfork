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
package net.bluemind.ui.adminconsole.directory.ou;

import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.directory.ou.l10n.OrgUnitConstants;

public class OUScreens {

	public static JsArray<ScreenElementContribution> getContributions() {
		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();
		contribs.push(
				ScreenElementContribution.create(null, null, ScreenRoot.create("ouBrowser", OrgUnitsBrowser.TYPE)));

		return contribs;

	}

	public static MenuContribution getMenuContribution() {
		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();

		screens.push(Contributed.create("dir2",
				Screen.create("ouBrowser", OrgUnitConstants.INST.browse(), BasicRoles.ROLE_SHOW_OU, false)
						.withOURoles(BasicRoles.ROLE_SHOW_OU)));

		return MenuContribution.create(sections, screens);

	}

}
