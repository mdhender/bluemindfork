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
package net.bluemind.gwtconsoleapp.base.menus;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.ui.extensions.gwt.UIExtension;
import net.bluemind.ui.extensions.gwt.UIExtensionPoint;
import net.bluemind.ui.extensions.gwt.UIExtensionsManager;

public class MenuContributors {

	public static List<MenuContributor> getContributors(String extensionPointId) {
		UIExtensionsManager manager = new UIExtensionsManager();
		UIExtensionPoint ep = manager.getExtensionPoint(extensionPointId);

		List<MenuContributor> ret = new ArrayList<>();
		for (UIExtension e : ep.getExtensions()) {
			String func = e.getConfigurationElements("contributor")[0].getAttribute("function");
			MenuContributor contributor = call(func);
			ret.add(contributor);
		}

		return ret;
	}

	private native static MenuContributor call(String func)
	/*-{
		return $wnd[func].apply();
	}-*/;
}
