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
package net.bluemind.ui.adminconsole.base.ui;

import java.util.HashMap;
import java.util.Map;

public class AppScreenRegistry {

	private static final AppScreenRegistry inst = new AppScreenRegistry();

	public static AppScreenRegistry get() {
		return inst;
	}

	private Map<String, IAppScreenFactory> factories = new HashMap<>();

	public void register(String screen, IAppScreenFactory factory) {
		factories.put(screen, factory);
	}

	public IAppScreenFactory get(String scr) {
		return factories.get(scr);
	}
}
