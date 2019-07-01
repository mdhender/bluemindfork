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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class ContributorUtil {

	public static MenuContribution mergeMenuContributions(MenuContribution... contribs) {
		JsArray<Contributed<Section>> sections = JsArray.createArray().cast();
		JsArray<Contributed<Screen>> screens = JsArray.createArray().cast();

		for (MenuContribution contrib : contribs) {
			mergeArray(sections, contrib.getSections());
			mergeArray(screens, contrib.getScreens());
		}

		return MenuContribution.create(sections, screens);
	}

	private static <T extends JavaScriptObject> void mergeArray(JsArray<T> dest, JsArray<T> source) {
		for (int i = 0; i < source.length(); i++) {
			dest.push(source.get(i));
		}

	}

}
