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
package net.bluemind.ui.adminconsole.directory.user;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.gwt.endpoint.AuthenticationGwtEndpoint;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.user.api.gwt.js.JsUser;

public class Sudo extends CompositeGwtWidgetElement implements IGwtWidgetElement {

	interface GenralUiBinder extends UiBinder<HTMLPanel, Sudo> {
	}

	public static final String TYPE = "bm.ac.Sudo";

	private static GenralUiBinder uiBinder = GWT.create(GenralUiBinder.class);
	public static final BBStyle style;
	public static final BBBundle bundle;

	@UiField
	FlowPanel availableApps;

	@UiField
	DivElement asTheUser;

	public interface BBBundle extends ClientBundle {

		@Source("Sudo.css")
		BBStyle getStyle();

	}

	static {
		bundle = GWT.create(BBBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();
	}

	public interface BBStyle extends CssResource {
		String appLink();
	}

	private Sudo() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		String domainUid = map.getString("domainUid");
		String userUid = map.getString("userId");
		JsUser user = map.get("user").cast();
		asTheUser.getStyle().setProperty("display", "block");
		String latd = user.getLogin() + "@" + domainUid;

		availableApps.add(createAppLink(userUid, latd, "../"));
	}

	private Widget createAppLink(final String currentUser, final String latd, final String app) {
		Anchor al = new Anchor("BlueMind");
		al.addStyleName(style.appLink());
		al.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				GWT.log("app '" + app + "'link");

				new AuthenticationGwtEndpoint(Ajax.TOKEN.getSessionId()).suWithParams(latd, true,
						new DefaultAsyncHandler<LoginResponse>() {

					@Override
					public void success(LoginResponse value) {
						GWT.log("sudo success: " + value.authKey);
						try {
							openApp(app, latd, value.authKey);
						} catch (RequestException e) {
							Notification.get().reportError(e);
						}

					}
				});
			}
		});
		return al;
	}

	private void openApp(final String app, final String latd, final String token) throws RequestException {
		StringBuilder url = new StringBuilder();
		url.append("/bluemind_sso_security");
		final String tgt = url.toString();
		RequestBuilder rb = new RequestBuilder(RequestBuilder.POST, tgt);

		StringBuilder params = new StringBuilder(255);
		params.append("priv=false");
		params.append("&login=" + latd);
		params.append("&password=" + token);
		String pms = params.toString();
		rb.setHeader("Content-Type", "multipart/form-data");

		rb.sendRequest(pms, new RequestCallback() {

			@Override
			public void onResponseReceived(Request request, Response response) {
				String cookie = response.getHeader("BMSsoCookie");
				if (cookie != null) {
					StringBuilder sb = new StringBuilder(255);
					sb.append('/').append(app).append('/');
					sb.append("?BMHPS=").append(cookie);
					String u = sb.toString();
					Window.Location.replace(u);
				}
			}

			@Override
			public void onError(Request request, Throwable e) {
				Notification.get().reportError(e);
			}
		});

	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new Sudo();
			}
		});
	}
}
