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
package net.bluemind.ui.adminconsole.directory.resource;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;

import net.bluemind.calendar.api.CalendarSettingsData;
import net.bluemind.calendar.api.gwt.endpoint.CalendarSettingsGwtEndpoint;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.api.IContainerManagementPromise;
import net.bluemind.core.container.api.gwt.endpoint.ContainerManagementGwtEndpoint;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.domain.api.gwt.endpoint.DomainSettingsGwtEndpoint;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.resource.api.IResourcesPromise;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.resource.api.gwt.endpoint.ResourcesGwtEndpoint;
import net.bluemind.resource.api.gwt.js.JsResourceDescriptor;
import net.bluemind.resource.api.gwt.serder.ResourceDescriptorGwtSerDer;
import net.bluemind.ui.adminconsole.directory.calendar.CalendarSettingsModelHandler;
import net.bluemind.ui.common.client.forms.Ajax;

public class QCreateResourceModelHandler implements IGwtModelHandler {
	public static final String TYPE = "bm.ac.QCreateResourceModelHandler";

	private QCreateResourceModelHandler() {
	}

	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final JsArrayString admins = map.get("res-admin-uids").cast();
		final JsItemValue<JsDomain> domain = map.get("domain").cast();
		final JsResourceDescriptor resource = map.get("rd").cast();
		IResourcesPromise resources = new ResourcesGwtEndpoint(Ajax.TOKEN.getSessionId(), domain.getUid()).promiseApi();
		final String uid = net.bluemind.ui.common.client.forms.tag.UUID.uuid();
		map.putString("id", uid);
		resources.create(uid, new ResourceDescriptorGwtSerDer().deserialize(new JSONObject(resource)))
				.thenCompose(v -> {
					IContainerManagementPromise cm = new ContainerManagementGwtEndpoint(Ajax.TOKEN.getSessionId(),
							"calendar:" + uid).promiseApi();

					final List<AccessControlEntry> entries = new ArrayList<>();
					for (int i = 0; i < admins.length(); i++) {
						String subject = admins.get(i);
						entries.add(AccessControlEntry.create(subject, Verb.All));
					}
					return cm.setAccessControlList(entries);
				}).thenCompose(v -> {
					return new DomainSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), domain.getUid()).promiseApi().get();
				})

				.thenCompose(settings -> {
					CalendarSettingsData calSettings = CalendarSettingsModelHandler.createCalendarSettings(settings);
					return new CalendarSettingsGwtEndpoint(Ajax.TOKEN.getSessionId(), "calendar:" + uid).promiseApi()
							.set(calSettings);
				}).whenComplete((a, e) -> {
					if (e != null) {
						handler.failure(e);
					} else {
						handler.success(null);
					}
				});
	}

	@Override
	public void load(JavaScriptObject model, AsyncHandler<Void> handler) {
		JsMapStringJsObject map = model.cast();
		map.put("rd",
				new ResourceDescriptorGwtSerDer().serialize(new ResourceDescriptor()).isObject().getJavaScriptObject());
		handler.success(null);
	}

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new QCreateResourceModelHandler();
			}
		});
		GWT.log("bm.ac.QCreateResourceModelHandler registred");
	}

}
