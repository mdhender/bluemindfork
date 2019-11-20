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
package net.bluemind.ui.adminconsole.system.maintenance.update;

import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.GwtStream;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.IInstallationAsync;
import net.bluemind.system.api.IInstallationPromise;
import net.bluemind.system.api.gwt.endpoint.InstallationEndpointPromise;
import net.bluemind.system.api.gwt.endpoint.InstallationGwtEndpoint;
import net.bluemind.system.api.gwt.endpoint.InstallationSockJsEndpoint;
import net.bluemind.system.api.gwt.js.JsSubscriptionInformations;
import net.bluemind.system.api.gwt.serder.SubscriptionInformationsGwtSerDer;
import net.bluemind.ui.adminconsole.base.SubscriptionInfoHolder;
import net.bluemind.ui.adminconsole.system.subscription.SubscriptionKeys;
import net.bluemind.ui.common.client.forms.Ajax;

public class UpdateModelHandler implements IGwtModelHandler {

	public static final String TYPE = "bm.ac.UpdateModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new UpdateModelHandler();
			}
		});
		GWT.log(TYPE + " registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		IInstallationPromise installationService = new InstallationEndpointPromise(
				new InstallationSockJsEndpoint(Ajax.TOKEN.getSessionId()));

		CompletableFuture<Void> subInfosLoad = installationService.getSubscriptionInformations().thenAccept(value -> {
			if (value != null) {
				JsSubscriptionInformations subscription = new SubscriptionInformationsGwtSerDer().serialize(value)
						.isObject().getJavaScriptObject().cast();
				map.put(SubscriptionKeys.subscription.name(), subscription);
			}
		});

		subInfosLoad.thenRun(() -> handler.success(null)).exceptionally(t -> {
			handler.failure(t);
			return null;
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		IInstallationAsync service = new InstallationGwtEndpoint(Ajax.TOKEN.getSessionId());
		final JsMapStringJsObject map = model.cast();
		String removeSubscription = map.getString("deleteSubscription");
		if (null != removeSubscription && removeSubscription.equals("true")) {
			service.removeSubscription(new DefaultAsyncHandler<Void>(handler) {

				@Override
				public void success(Void value) {
					handler.success(null);
					SubscriptionInfoHolder.get().init();
				}

			});
		} else {
			String subscription = map.getString(SubscriptionKeys.license.name());
			String subscriptionFilename = map.getString(SubscriptionKeys.filename.name());

			DefaultAsyncHandler<Void> defaultHandler = new DefaultAsyncHandler<Void>(handler) {

				@Override
				public void success(Void value) {
					handler.success(null);
					SubscriptionInfoHolder.get().init();

				}
			};

			if (subscriptionFilename.endsWith(".bmz")) {
				service.updateSubscriptionWithArchive(new GwtStream(subscription), defaultHandler);
			} else {
				service.updateSubscription(subscription, defaultHandler);
			}
		}
	}

}
