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
package net.bluemind.ui.adminconsole.system.domains.edit;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Timer;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.gwt.endpoint.TaskGwtEndpoint;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.endpoint.DomainsGwtEndpoint;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.system.domains.DomainKeys;
import net.bluemind.ui.common.client.forms.Ajax;

public class DomainModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.DomainModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new DomainModelHandler();
			}
		});
		GWT.log("bm.ac.DomainModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		loadDomain(handler, map);
	}

	private void loadDomain(final AsyncHandler<Void> handler, final JsMapStringJsObject map) {
		final String domainUid = map.getString(DomainKeys.domainUid.name());
		DomainsGwtEndpoint service = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());

		service.get(domainUid, new DefaultAsyncHandler<ItemValue<Domain>>(handler) {

			@Override
			public void success(final ItemValue<Domain> domainItemValue) {
				JSONValue jsDomainItem = new ItemValueGwtSerDer<Domain>(new DomainGwtSerDer())
						.serialize(domainItemValue);
				map.put(DomainKeys.domainItem.name(), jsDomainItem.isObject().getJavaScriptObject());
				map.putString(DomainKeys.domainUid.name(), domainItemValue.uid);
				JsDomain jsDomain = new DomainGwtSerDer().serialize(domainItemValue.value).isObject()
						.getJavaScriptObject().cast();
				map.put(DomainKeys.domain.name(), jsDomain);

				handler.success(null);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		final String domainUid = map.getString(DomainKeys.domainUid.name());
		JsDomain jsDomain = map.get(DomainKeys.domain.name()).cast();
		final Domain domain = new DomainGwtSerDer().deserialize(new JSONObject(jsDomain));

		DomainsGwtEndpoint service = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());
		service.update(domainUid, domain, new DefaultAsyncHandler<Void>(handler) {

			@Override
			public void success(Void value) {
				saveAliases(handler, map, domainUid);
			}
		});

	}

	private void saveAliases(final AsyncHandler<Void> handler, final JsMapStringJsObject map, final String domainUid) {
		final DomainsGwtEndpoint service = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());
		Set<String> aliases = new HashSet<>();
		JSONArray aliasesArray = new JSONArray(map.get(DomainKeys.aliases.name()));
		for (int i = 0; i < aliasesArray.size(); i++) {
			aliases.add(aliasesArray.get(i).isString().stringValue());
		}

		service.setAliases(domainUid, aliases, new DefaultAsyncHandler<TaskRef>(handler) {

			@Override
			public void success(TaskRef value) {
				TaskGwtEndpoint taskService = new TaskGwtEndpoint(Ajax.TOKEN.getSessionId(), String.valueOf(value.id));
				waitForTaskRef(taskService);
			}

			private void waitForTaskRef(final TaskGwtEndpoint taskService) {

				taskService.status(new DefaultAsyncHandler<TaskStatus>(handler) {

					@Override
					public void success(TaskStatus status) {
						if (status.state.ended) {
							if (status.state.succeed) {
								successMessage(domainUid);
								handler.success(null);
							} else {
								handler.failure(
										new RuntimeException("Cannot save domain aliases: " + status.lastLogEntry));
							}
						} else {
							Timer t = new Timer() {
								@Override
								public void run() {
									waitForTaskRef(taskService);
								}
							};
							t.schedule(500);
						}
					}
				});
			}

		});
	}

	private void successMessage(String domainUid) {
		Notification.get().reportInfo("Domain " + domainUid + " saved");
	}

}
