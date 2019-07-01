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

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.calendar.api.gwt.endpoint.FreebusyMgmtGwtEndpoint;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainersAsync;
import net.bluemind.core.container.api.gwt.endpoint.ContainersGwtEndpoint;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class CalendarManagementModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.settings.CalendarManagementModelHandler";

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {

		String owner = ((JsMapStringString) model.cast()).get("userId");

		final CalendarManagementModel cmm = model.cast();

		IContainersAsync containers = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId());
		ContainerQuery query = ContainerQuery.type("calendar");
		query.owner = owner;

		containers.all(query, new DefaultAsyncHandler<List<ContainerDescriptor>>(handler) {

			@Override
			public void success(List<ContainerDescriptor> value) {
				cmm.setCalendars(value);

				if (Ajax.TOKEN.getSubject().contains("global.virt")) {
					handler.success(null);
					return;
				}
				FreebusyMgmtGwtEndpoint fb = new FreebusyMgmtGwtEndpoint(Ajax.TOKEN.getSessionId(),
						"freebusy:" + Ajax.TOKEN.getSubject());
				fb.get(new AsyncHandler<List<String>>() {

					@Override
					public void success(List<String> value) {
						if (value != null) {
							cmm.setFreebusy(value);
						}
						handler.success(null);
					}

					@Override
					public void failure(Throwable e) {
						GWT.log("Cannot load freebusy container : " + e);
						handler.success(null);
					}
				});
			}

		});

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		if (Ajax.TOKEN.getSubject().contains("global.virt")) {
			handler.success(null);
			return;
		}

		CalendarManagementModel cmm = model.cast();
		FreebusyMgmtGwtEndpoint fb = new FreebusyMgmtGwtEndpoint(Ajax.TOKEN.getSessionId(),
				"freebusy:" + Ajax.TOKEN.getSubject());

		fb.set(cmm.getFreebusyAsList(), new DefaultAsyncHandler<Void>(handler) {

			@Override
			public void success(Void value) {
				handler.success(null);
			}
		});

	}

	public static void registerType() {
		GwtModelHandler.register(TYPE,
				new IGwtDelegateFactory<IGwtModelHandler, net.bluemind.gwtconsoleapp.base.editor.ModelHandler>() {

					@Override
					public IGwtModelHandler create(net.bluemind.gwtconsoleapp.base.editor.ModelHandler model) {
						return new CalendarManagementModelHandler();
					}
				});

	}

}
