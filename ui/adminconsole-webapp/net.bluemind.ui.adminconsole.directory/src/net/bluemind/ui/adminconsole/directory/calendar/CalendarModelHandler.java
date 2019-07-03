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
package net.bluemind.ui.adminconsole.directory.calendar;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.gwt.endpoint.CalendarsMgmtGwtEndpoint;
import net.bluemind.calendar.api.gwt.js.JsCalendarDescriptor;
import net.bluemind.calendar.api.gwt.serder.CalendarDescriptorGwtSerDer;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class CalendarModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.CalendarModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new CalendarModelHandler();
			}
		});
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("calendarId");
		String domainUid = map.getString("domainUid");

		CalendarsMgmtGwtEndpoint dcb = new CalendarsMgmtGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

		dcb.getComplete(s, new DefaultAsyncHandler<CalendarDescriptor>(handler) {

			@Override
			public void success(CalendarDescriptor value) {
				map.put("calendar",
						new CalendarDescriptorGwtSerDer().serialize(value).isObject().getJavaScriptObject());
				parentHandler.success(null);
			}

		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		String s = map.getString("calendarId");
		String domainUid = map.getString("domainUid");
		CalendarsMgmtGwtEndpoint dcb = new CalendarsMgmtGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		JsCalendarDescriptor ab = map.get("calendar").cast();
		dcb.update(s, new CalendarDescriptorGwtSerDer().deserialize(new JSONObject(ab)), handler);
	}

}
