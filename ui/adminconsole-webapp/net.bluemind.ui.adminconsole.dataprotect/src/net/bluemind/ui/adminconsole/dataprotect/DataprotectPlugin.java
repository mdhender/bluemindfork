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

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;

public class DataprotectPlugin {

	public static void init() {
		GWT.log("DataProtect plugin init()");
		DPNavigator.registerType();
		GenerationBrowser.registerType();
		DPPolicy.registerType();

		MenuContributor.exportAsfunction("NetBluemindUiAdminconsoleDataprotectContributor",
				MenuContributor.create(new DataprotectMenusContributor()));
		ScreenElementContributor.exportAsfunction("NetBluemindUiAdminconsoleDataprotectScreensContributor",
				ScreenElementContributor.create(new DataprotectScreensContributor()));

	}
}
