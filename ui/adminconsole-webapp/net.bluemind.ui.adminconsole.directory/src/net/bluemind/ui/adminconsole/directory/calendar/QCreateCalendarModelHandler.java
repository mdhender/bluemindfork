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

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.gwt.endpoint.CalendarSettingsGwtEndpoint;
import net.bluemind.calendar.api.gwt.endpoint.CalendarsMgmtGwtEndpoint;
import net.bluemind.calendar.api.gwt.js.JsCalendarDescriptor;
import net.bluemind.calendar.api.gwt.serder.CalendarDescriptorGwtSerDer;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.domain.api.gwt.endpoint.DomainSettingsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.common.client.forms.Ajax;

public class QCreateCalendarModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.QCreateCalendarModelHandler";

	private QCreateCalendarModelHandler() {
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateCalendarModelHandler();
			}
		});
	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {

		GWT.log("initialize QCreateCalendarModel");
		JsMapStringJsObject map = model.cast();
		CalendarDescriptor cal = new CalendarDescriptor();
		map.put("calendar", new CalendarDescriptorGwtSerDer().serialize(cal).isObject().getJavaScriptObject());

		if (DomainsHolder.get().getSelectedDomain() != null) {
			map.putString("domainUid", DomainsHolder.get().getSelectedDomain().uid);
		}
		handler.success(null);
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();

		final String domainUid = map.getString("domainUid");
		JsCalendarDescriptor book = map.get("calendar").cast();
		book.setOwner(domainUid);
		book.setDomainUid(domainUid);
		CalendarsMgmtGwtEndpoint dc = new CalendarsMgmtGwtEndpoint(Ajax.TOKEN.getSessionId());
		final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
		map.putString("calendarUid", uid);
		dc.create(uid, new CalendarDescriptorGwtSerDer().deserialize(new JSONObject(book)),
				new DefaultAsyncHandler<Void>(handler) {

					@Override
					public void success(Void value) {
						new DomainSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid)
								.get(new DefaultAsyncHandler<Map<String, String>>(handler) {

									@Override
									public void success(Map<String, String> value) {
										CalendarSettingsData calSettings = CalendarSettingsModelHandler
												.createCalendarSettings(value);
										if (null != calSettings) {
											new CalendarSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), uid)
													.set(calSettings, handler);
										} else {
											handler.success(null);
										}
									}

								});
					}
				});
	}

}
