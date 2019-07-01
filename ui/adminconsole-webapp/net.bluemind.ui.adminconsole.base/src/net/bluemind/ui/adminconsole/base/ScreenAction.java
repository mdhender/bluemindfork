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
package net.bluemind.ui.adminconsole.base;

import net.bluemind.gwtconsoleapp.base.menus.Screen;
import net.bluemind.ui.adminconsole.base.ui.AppScreen;
import net.bluemind.ui.adminconsole.base.ui.IAppScreenFactory;
import net.bluemind.ui.adminconsole.base.ui.ScreenContext;

public class ScreenAction {

	private IAppScreenFactory screenFactory;
	private Screen screen;

	public ScreenAction(IAppScreenFactory screenFactory, Screen screen) {
		this.screenFactory = screenFactory;
		this.screen = screen;
	}

	public AppScreen appScreen(ScreenContext sc) {
		return screenFactory.create(screen);
	}
}
