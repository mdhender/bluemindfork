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
package net.bluemind.ui.adminconsole.directory.externaluser;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.directory.externaluser.l10n.ExternalUserMenusConstants;

public class ExternalUserMenus {

	public static MenuContribution getContribution() {

		ExternalUserMenusConstants v = GWT
				.create(ExternalUserMenusConstants.class);
		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();

		screens.push(Contributed.create("directories",
				Screen.createDirEditor("editExternalUser", v.editExternalUser(),
						null, false)));

		screens.push(Contributed.create("dir1",
				Screen.create("qcExternalUser", v.qcExternalUser(),
						BasicRoles.ROLE_MANAGE_EXTERNAL_USER, false)
						.withRoles(BasicRoles.ROLE_MANAGE_EXTERNAL_USER)));

		return MenuContribution.create(sections, screens);

	}
}
