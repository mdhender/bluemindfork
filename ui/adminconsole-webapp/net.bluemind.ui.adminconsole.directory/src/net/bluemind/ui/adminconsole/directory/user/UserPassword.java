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

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.i18n.shared.DateTimeFormat.PredefinedFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.directory.user.l10n.UserConstants;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.ChangePassword;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;
import net.bluemind.user.api.gwt.js.JsUser;

public class UserPassword extends CompositeGwtWidgetElement implements IGwtWidgetElement {

	interface GenralUiBinder extends UiBinder<HTMLPanel, UserPassword> {
	}

	public static final String TYPE = "bm.ac.UserPassword";

	private static GenralUiBinder uiBinder = GWT.create(GenralUiBinder.class);

	private UserPassword() {
		HTMLPanel panel = uiBinder.createAndBindUi(this);
		initWidget(panel);
	}

	@UiField
	PasswordTextBox password;

	@UiField
	PasswordTextBox confirmPassword;

	@UiField
	Label passwordLastChange;

	@UiField
	CheckBox passwordMustChange;

	@UiField
	CheckBox passwordNeverExpires;

	private String userUid;

	private String domainUid;

	@UiHandler("changePassword")
	void handleClick(ClickEvent e) {
		if (!checkPasswordFields()) {
			Notification.get().reportError(UserConstants.INST.passwordMismatch());
			return;
		} else {
			UserGwtEndpoint ep = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid);

			ep.setPassword(userUid, ChangePassword.create(password.asEditor().getValue()), new AsyncHandler<Void>() {

				@Override
				public void success(Void value) {
					// FIXME i18n
					Notification.get().reportInfo("Password changed !");
					passwordLastChange
							.setText(DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_FULL).format(new Date()));
				}

				@Override
				public void failure(Throwable e) {
					if (e instanceof ServerFault && ((ServerFault) e).getCode() == ErrorCode.FORBIDDEN) {
						Notification.get().reportError(new ServerFault(e.getMessage()));
						return;
					}

					Notification.get().reportError(e);
				}
			});
		}
	}

	private boolean checkPasswordFields() {
		String p = password.asEditor().getValue();
		String cp = confirmPassword.asEditor().getValue();

		if (p == null || p.length() == 0) {
			return true;
		}
		return p.equals(cp);

	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		domainUid = map.getString("domainUid");
		userUid = map.getString("userId");

		if (map.get("user") == null) {
			GWT.log("user not found..");
			return;
		}

		final JsUser user = map.get("user").cast();
		if (user.getPasswordLastChange() == null) {
			passwordLastChange.setText("-");
		} else {
			passwordLastChange.setText(DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_FULL)
					.format(new Date((long) user.getPasswordLastChange().getTime())));
		}

		passwordMustChange.setValue(user.getPasswordMustChange());
		passwordNeverExpires.setValue(user.getPasswordNeverExpires());
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		final JsUser user = map.get("user").cast();
		user.setPasswordMustChange(passwordMustChange.getValue());
		user.setPasswordNeverExpires(passwordNeverExpires.getValue());
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new UserPassword();
			}
		});
	}
}
