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
package net.bluemind.ui.settings.client.forms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.user.api.ChangePassword;
import net.bluemind.user.api.IUserAsync;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;

public class PasswordEdit extends CompositeGwtWidgetElement {

	public final static String TYPE = "bm.settings.UserPassword";
	private static final PasswordEditConstants constants = GWT.create(PasswordEditConstants.class);

	private PasswordTextBox current;
	private PasswordTextBox password;
	private PasswordTextBox confirmation;
	private Button btn;
	private FlexTable pwdContainer;
	private Label currentErrLabel;
	private Label newErrLabel;

	private FlexTable table;

	public PasswordEdit() {
		super();
		table = new FlexTable();
		table.setStyleName("formContainer");
		initWidget(table);

		pwdContainer = new FlexTable();

		// inputs
		current = new PasswordTextBox();
		currentErrLabel = new Label();
		currentErrLabel.setStyleName("errorMsg");
		password = new PasswordTextBox();
		confirmation = new PasswordTextBox();
		newErrLabel = new Label();
		newErrLabel.setStyleName("errorMsg");

		// button
		btn = new Button(constants.updatePassword());
		btn.setStyleName("button");
		btn.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				setInitialState(false);
				if (current.getValue().isEmpty()) {
					setCurrentPasswordError(constants.emptyCurrentPassword());
				} else if (password.getValue().isEmpty() || confirmation.getValue().isEmpty()) {
					setNewPasswordError(constants.emptyNewPassword());
				} else if (!password.getValue().equals(confirmation.getValue())) {
					setNewPasswordError(constants.diffGivenPassword());
				} else {
					savePassword(current.getValue(), password.getValue());
				}
			}
		});

		int i = 0;
		pwdContainer.setWidget(i, 0, new Label(constants.currentPassword()));
		pwdContainer.setWidget(i, 1, current);
		pwdContainer.setWidget(i, 2, currentErrLabel);

		i++;
		pwdContainer.setWidget(i, 0, new Label(constants.password()));
		pwdContainer.setWidget(i, 1, password);
		pwdContainer.setWidget(i, 2, newErrLabel);
		pwdContainer.getFlexCellFormatter().setRowSpan(i, 2, 2);

		i++;
		pwdContainer.setWidget(i, 0, new Label(constants.confirmPassword()));
		pwdContainer.setWidget(i, 1, confirmation);
		i++;
		pwdContainer.setWidget(i, 1, btn);

		table.setWidget(i, 0, new Label(constants.password()));
		table.setWidget(i, 1, pwdContainer);
		table.getRowFormatter().setStyleName(i, "setting");
		table.getCellFormatter().setStyleName(i, 0, "label");
		table.getCellFormatter().setStyleName(i, 1, "form");

	}

	protected void savePassword(String oldValue, String newValue) {
		IUserAsync userService = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), Ajax.TOKEN.getContainerUid());

		userService.setPassword(Ajax.TOKEN.getSubject(), ChangePassword.create(oldValue, newValue),
				new AsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						Notification.get().reportInfo(constants.passwordChanged());
					}

					@Override
					public void failure(Throwable e) {

						if (e instanceof ServerFault && ((ServerFault) e).getCode() == ErrorCode.AUTHENTICATION_FAIL) {
							setCurrentPasswordError(PasswordEditConstants.INST.wrongCurrentPassword());
						} else if (e instanceof ServerFault
								&& ((ServerFault) e).getCode() == ErrorCode.INVALID_PASSWORD) {
							setNewPasswordError(e.getMessage());
						} else {
							Notification.get().reportError(e);
						}

					}
				});
	}

	public void setInitialState(boolean resetForm) {
		currentErrLabel.setText("");
		newErrLabel.setText("");
		current.removeStyleName("error");
		password.removeStyleName("error");
		confirmation.removeStyleName("error");
		if (resetForm) {
			current.setText(null);
			password.setText(null);
			confirmation.setText(null);
		}
	}

	public void setCurrentPasswordError(String msg) {
		currentErrLabel.setText(msg);
		current.addStyleName("error");
		current.setFocus(true);
	}

	public void setNewPasswordError(String msg) {
		newErrLabel.setText(msg);
		password.addStyleName("error");
		confirmation.addStyleName("error");
		password.setFocus(true);
	}

	public static void registerType() {
		GwtWidgetElement.register(TYPE, new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement e) {
				return new PasswordEdit();
			}
		});
	}
}
