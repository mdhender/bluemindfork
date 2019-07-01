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
package net.bluemind.ui.gwtuser.client;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarUids.UserCalendarType;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.gwtsharing.client.BaseSharingEditor;

public class UserCalendarSharingEditor extends BaseSharingEditor {

	protected UserCalendarSharingEditor() {
		super("calendar-sharing", "calendar");
	}

	@Override
	protected String getContainerUid(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		return ICalendarUids.TYPE + ":" + UserCalendarType.Default + ":" + map.getString("userId");
	}

	public static void registerType() {
		GwtWidgetElement.register("bm.ac.UserCalendarSharingEditor",
				new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

					@Override
					public IGwtWidgetElement create(WidgetElement e) {
						return new UserCalendarSharingEditor();
					}
				});
	}

}
