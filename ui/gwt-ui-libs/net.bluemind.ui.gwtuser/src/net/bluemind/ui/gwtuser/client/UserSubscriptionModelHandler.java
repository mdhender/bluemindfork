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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.commons.gwt.JsMapStringString;
import net.bluemind.core.container.api.ContainerSubscription;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.api.IContainersAsync;
import net.bluemind.core.container.api.gwt.endpoint.ContainersGwtEndpoint;
import net.bluemind.core.container.api.gwt.serder.ContainerSubscriptionGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.IUserSubscriptionAsync;
import net.bluemind.user.api.gwt.endpoint.UserSubscriptionGwtEndpoint;

public class UserSubscriptionModelHandler implements IGwtModelHandler {

	private static final IContainersAsync containers = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId());

	private Map<String, Boolean> localModel;

	private String userId;

	private String domainUid;

	private String containerType;

	private String modelKey;

	private IUserSubscriptionAsync userSubscription;

	public UserSubscriptionModelHandler(String containerType) {
		this.containerType = containerType;
		this.modelKey = containerType + "-subscription";
	}

	@Override
	public void load(final JavaScriptObject model, final AsyncHandler<Void> handler) {
		JsMapStringString map = model.<JsMapStringString>cast();
		userId = map.get("userId");
		domainUid = map.get("domainUid");
		userSubscription = new UserSubscriptionGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);
		userSubscription.listSubscriptions(userId, containerType,
				new AsyncHandler<List<ContainerSubscriptionDescriptor>>() {

					@Override
					public void success(List<ContainerSubscriptionDescriptor> value) {
						Map<String, Boolean> ret = new HashMap<String, Boolean>();
						for (ContainerSubscriptionDescriptor d : value) {
							ret.put(d.containerUid, d.offlineSync);
						}
						UserSubscriptionModelHandler.this.localModel = ret;
						handler.success(null);
					}

					@Override
					public void failure(Throwable e) {
						handler.failure(e);
					}
				});

	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		Map<String, Boolean> current = localModel;

		JsMapStringJsObject m = model.cast();
		JSONArray jsArray = new JSONArray(m.get(modelKey));
		Map<String, Boolean> fromModel = new HashMap<String, Boolean>();
		ContainerSubscriptionGwtSerDer serder = new ContainerSubscriptionGwtSerDer();
		for (int i = 0; i < jsArray.size(); i++) {
			ContainerSubscription cs = serder.deserialize(jsArray.get(i));
			fromModel.put(cs.containerUid, cs.offlineSync);
		}

		List<ContainerSubscription> toAddOrUpdate = new ArrayList<ContainerSubscription>();
		for (String uid : fromModel.keySet()) {
			if (!current.containsKey(uid)) {
				toAddOrUpdate.add(ContainerSubscription.create(uid, fromModel.get(uid)));
			} else {
				if (current.get(uid) != fromModel.get(uid)) {
					toAddOrUpdate.add(ContainerSubscription.create(uid, fromModel.get(uid)));
				}
			}
		}

		List<String> toDelete = new ArrayList<String>();
		for (String uid : current.keySet()) {
			if (!fromModel.containsKey(uid)) {
				toDelete.add(uid);
			}

		}

		if (toDelete.isEmpty() && toAddOrUpdate.isEmpty()) {
			// nothing to do
			handler.success(null);
			return;
		}

		doSubUnsub(toAddOrUpdate, toDelete, handler);
	}

	private void doSubUnsub(List<ContainerSubscription> toSub, List<String> toDelete,
			final AsyncHandler<Void> handler) {
		if (toSub.size() > 0) {
			userSubscription.subscribe(userId, toSub, new DefaultAsyncHandler<Void>(handler) {

				@Override
				public void success(Void value) {
				}
			});

		}

		if (toDelete.size() > 0) {
			userSubscription.unsubscribe(userId, toDelete, new DefaultAsyncHandler<Void>(handler) {

				@Override
				public void success(Void value) {
				}
			});
		}

		handler.success(null);
	}

}