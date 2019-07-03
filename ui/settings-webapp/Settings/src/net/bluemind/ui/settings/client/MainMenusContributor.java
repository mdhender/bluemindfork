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
package net.bluemind.ui.settings.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.menus.Contributed;
import net.bluemind.gwtconsoleapp.base.menus.MenuContribution;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributorUnwrapped;
import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.gwtconsoleapp.base.menus.Section;
import net.bluemind.ui.settings.client.about.AboutConstants;
import net.bluemind.ui.settings.client.forms.apikeys.AKConstants;
import net.bluemind.ui.settings.client.myaccount.AccountMessages;

public class MainMenusContributor implements MenuContributorUnwrapped {

	public static final AccountMessages messages = GWT.create(AccountMessages.class);

	@Override
	public MenuContribution contribution() {

		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		sections.push(Contributed.create(null,
				Section.create("about", AboutConstants.INST.anchor(), 1, null,
						JsArray.createArray().<JsArray<Screen>> cast(), //
						JsArray.createArray().<JsArray<Section>> cast())));

		sections.push(Contributed.create(null,
				Section.create("apiKeys", AKConstants.INST.appName(), 2, null,
						JsArray.createArray().<JsArray<Screen>> cast(), //
						JsArray.createArray().<JsArray<Section>> cast())));

		return MenuContribution.create(sections, JsArray.createArray().<JsArray<Contributed<Screen>>> cast());
	}
}
