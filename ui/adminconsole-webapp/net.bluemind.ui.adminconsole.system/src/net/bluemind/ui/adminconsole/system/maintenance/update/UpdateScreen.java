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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.gwt.endpoint.InstallationGwtEndpoint;
import net.bluemind.system.api.gwt.serder.SubscriptionInformationsGwtSerDer;
import net.bluemind.ui.adminconsole.system.maintenance.update.l10n.UpdateConstants;
import net.bluemind.ui.adminconsole.system.subscription.SubscriptionKeys;

public class UpdateScreen extends Composite implements IGwtScreenRoot {

	private static final String TYPE = "bm.ac.UpdateScreen";

	private ScreenRoot screenRoot;

	interface UpdateBinder extends UiBinder<HTMLPanel, UpdateScreen> {
	}

	@UiField
	HTMLPanel subscriptionUnavailable;

	@UiField
	HTMLPanel subscriptionAvailable;

	@UiField
	Label majorVersion;

	@UiField
	Button setupUpdate;

	private static UpdateBinder uiBinder = GWT.create(UpdateBinder.class);

	private UpdateScreen(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		HTMLPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new UpdateScreen(screenRoot);
			}
		});
	}

	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
		instance.load(new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
			}
		});
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		final JsMapStringJsObject map = model.cast();

		JavaScriptObject jsSubscription = map.get(SubscriptionKeys.subscription.name());

		SubscriptionInformations sub = null;
		if (jsSubscription != null) {
			sub = new SubscriptionInformationsGwtSerDer().deserialize(new JSONObject(jsSubscription.cast()));
			if (sub.valid) {
				GWT.log("Found an installed subscription");
				subscriptionAvailable(sub);
			} else {
				GWT.log("Invalid subscription");
				subscriptionUnavailable();
			}
		} else {
			GWT.log("Unable to get subscription from core!");
			subscriptionUnavailable();
		}
	}

	private void subscriptionUnavailable() {
		subscriptionUnavailable.setVisible(true);
		subscriptionAvailable.setVisible(false);
	}

	private void subscriptionAvailable(SubscriptionInformations sub) {
		subscriptionUnavailable.setVisible(false);
		subscriptionAvailable.setVisible(true);
		majorVersion.setText(sub.version);
	}

	@UiHandler("setupUpdate")
	void setupUpdateClickHandler(ClickEvent ce) {
		InstallationGwtEndpoint service = new InstallationGwtEndpoint(Ajax.TOKEN.getSessionId());
		service.updateSubscriptionVersion("latest", new DefaultAsyncHandler<Void>() {
			@Override
			public void success(Void value) {
				DialogBox overlay = new DialogBox();
				overlay.addStyleName("dialog");
				overlay.setWidget(new Label(UpdateConstants.INST.continuUpdateHelp()));
				overlay.setGlassEnabled(true);
				overlay.setAutoHideEnabled(true);
				overlay.setGlassStyleName("modalOverlay");
				overlay.setModal(true);
				overlay.center();
				overlay.show();
			}
		});
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("updateBluemind", TYPE).cast();
		screenRoot.getHandlers().push(ModelHandler.create(null, UpdateModelHandler.TYPE).<ModelHandler>cast());
		return screenRoot;
	}

}
