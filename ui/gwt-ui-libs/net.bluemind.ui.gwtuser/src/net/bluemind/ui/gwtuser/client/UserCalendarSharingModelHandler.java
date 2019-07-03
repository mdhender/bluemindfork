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
import com.google.gwt.core.shared.GWT;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarUids.UserCalendarType;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.ui.gwtsharing.client.BaseSharingModelHandler;

public class UserCalendarSharingModelHandler extends BaseSharingModelHandler {

	public UserCalendarSharingModelHandler() {
		super("calendar-sharing");
	}

	public static void registerType() {
		GwtModelHandler.register("bm.ac.UserCalendarSharingModelHandler",
				new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

					@Override
					public IGwtModelHandler create(ModelHandler modelHandler) {
						return new UserCalendarSharingModelHandler();
					}
				});
		GWT.log("bm.ac.CalendarSharingModelHandler registred");
	}

	@Override
	protected String getContainerUid(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		return ICalendarUids.TYPE + ":" + UserCalendarType.Default + ":" + map.getString("userId");
	}
}
