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

public class MenuContribution extends JavaScriptObject {
	protected MenuContribution() {
	}

	public native final JsArray<Contributed<Section>> getSections()
	/*-{
	return this.sections;
	}-*/;

	public native final JsArray<Contributed<Screen>> getScreens()
	/*-{
	return this.screens;
	}-*/;

	public static native final MenuContribution create(JsArray<Contributed<Section>> sections,
			JsArray<Contributed<Screen>> screens)
			/*-{
			return {
			  'sections' : sections,
			  'screens' : screens
			};
			}-*/;
}
