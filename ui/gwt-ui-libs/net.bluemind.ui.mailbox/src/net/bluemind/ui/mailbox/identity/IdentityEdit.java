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
package net.bluemind.ui.mailbox.identity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.CommonForm;
import net.bluemind.ui.common.client.forms.extensions.ICommonEditor;
import net.bluemind.ui.editor.client.Editor;

public class IdentityEdit extends CommonForm implements ICommonEditor {

	public static interface Resources extends ClientBundle {
		@Source("IdentityEdit.css")
		Style editStyle();
	}

	public static interface Style extends CssResource {

		String identities();

		String headers();

		String name();

		String label();

		String value();

		String action();

		String identity();

		String current();

		String actionCell();

	}

	interface IdentityEditUiBinder extends UiBinder<HTMLPanel, IdentityEdit> {

	}

	private static IdentityEditUiBinder binder = GWT.create(IdentityEditUiBinder.class);

	/**
	 * Emails combo box displayed like 'sumptuous.address@princess.com
	 * (balpartagee)'.
	 */
	@UiField
	ListBox emails;

	/**
	 * Because {@link #emails} does not hold the raw email address, we store it
	 * here.
	 */
	private Map<Integer, String> rawEmailByEmailComboboxIndex = new HashMap<>();

	@UiField
	TextBox email;

	@UiField
	Editor sigContent;

	@UiField
	TextBox displayname;

	@UiField
	TextBox name;

	@UiField
	CheckBox format;

	@UiField
	CheckBox unrestricted;

	@UiField
	CheckBox sent;

	private List<IdentityDescription> templates;

	private static final Resources res = GWT.create(Resources.class);

	private final Style s;

	private Identity identity;

	private boolean supportsExternalIdentities;

	/**
	 * @param supportsExternalIdentities
	 * @param identity2
	 * @param templates2
	 */
	public IdentityEdit(Identity identity, List<IdentityDescription> templates, boolean supportsExternalIdentities) {
		this.identity = identity;
		this.templates = templates;
		this.supportsExternalIdentities = supportsExternalIdentities;
		s = res.editStyle();
		loadUI();
	}

	private void loadUI() {
		form = binder.createAndBindUi(this);

		email.setVisible(false);
		unrestricted.setValue(false);

		format.setValue(true);
		sent.setVisible(true);
		sent.setValue(true);
		s.ensureInjected();

		emails.getElement().setId("identity-emails");
		email.getElement().setId("identity-email");
		displayname.getElement().setId("identity-displayname");
		name.getElement().setId("identity-name");
		format.getElement().setId("identity-format");
		sent.getElement().setId("identity-sent");
		unrestricted.getElement().setId("identity-restricted");

		if (supportsExternalIdentities && Ajax.TOKEN.getRoles().contains(BasicRoles.ROLE_EXTERNAL_IDENTITY)) {
			unrestricted.setEnabled(true);
			unrestricted.setVisible(true);
		} else {
			unrestricted.setEnabled(false);
			unrestricted.setVisible(false);
		}

		emails.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				setFormTemplate();
			}

		});

		unrestricted.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				if (event.getValue()) {
					email.setVisible(true);
					emails.setVisible(false);
				} else {
					email.setVisible(false);
					emails.setVisible(true);
				}
			}
		});

		format.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

			@Override
			public void onValueChange(ValueChangeEvent<Boolean> event) {
				if (sigContent.getText().trim().isEmpty() || Window.confirm(getTexts().toggleEditor())) {
					if (event.getValue()) {
						sigContent.htmlEditor();
					} else {
						sigContent.plainEditor();
					}
				} else {
					format.setValue(!event.getValue());
				}
			}
		});

		int i = 0;
		for (IdentityDescription id : this.templates) {
			emails.insertItem(id.email + " (" + id.name + ")", i);
			this.rawEmailByEmailComboboxIndex.put(i, id.email);
			i++;
		}
		setFormData();
	}

	private void setSignatureFormat(SignatureFormat format) {
		if (format.equals(SignatureFormat.HTML) != this.format.getValue().booleanValue()) {
			this.format.setValue(format.equals(SignatureFormat.HTML));
			if (this.format.getValue()) {
				sigContent.htmlEditor();
			} else {
				sigContent.plainEditor();
			}
		}
	}

	public boolean save() {
		if (validate()) {
			identity.email = getEmail();
			identity.format = format.getValue() ? SignatureFormat.HTML : SignatureFormat.PLAIN;
			identity.signature = sigContent.getText();
			identity.displayname = displayname.getText();
			identity.name = name.getText();
			identity.sentFolder = sent.getValue().booleanValue() ? "" : "Sent";
			return true;
		}
		return false;
	}

	private String getEmail() {
		final String email;
		if (unrestricted.getValue()) {
			email = this.email.getValue();
		} else {
			int idx = emails.getSelectedIndex();
			email = this.rawEmailByEmailComboboxIndex.get(idx);
		}
		return email;
	}

	private boolean validate() {
		if (unrestricted.getValue()) {
			String mail = email.getValue();
			String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.(?:[a-zA-Z]{2,6})$";
			if (!mail.matches(emailPattern)) {
				Window.alert(IdentityConstants.INST.invalidEmail());
				return false;
			}
		}

		String n = name.asEditor().getValue();
		if (n == null || n.isEmpty()) {
			Notification.get().reportError(IdentityConstants.INST.invalidName());
			return false;
		}
		return true;
	}

	private void setFormTemplate() {
		int index = emails.getSelectedIndex();
		IdentityDescription id = templates.get(index);
		if (name.getText() == null || name.getText().isEmpty()) {
			name.setText(id.name);
		}
		sigContent.setText(id.signature);

		sent.setText(IdentityConstants.INST.useEntitySent(id.email + " (" + id.name + ")"));
		sent.setVisible(true);
	}

	private void setFormData() {
		setSignatureFormat(identity.format);
		sigContent.setText(identity.signature);
		displayname.setText(identity.displayname);
		name.setText(identity.name);
		setEmail(identity.email);
		sent.setValue(!"Sent".equals(identity.sentFolder));
		sent.setText(IdentityConstants.INST.useEntitySent(identity.email + " (" + identity.email + ")"));
		sent.setVisible(true);
	}

	private void setEmail(String email) {
		if (null == email || email.isEmpty()) {
			setUnrestricted(false);
		}
		for (int i = 0; i < templates.size(); i++) {
			if (templates.get(i).email.equals(email)) {
				emails.setSelectedIndex(i);
				setUnrestricted(false);
				return;
			}
		}
		this.email.setValue(email);
		setUnrestricted(true);
	}

	private void setUnrestricted(boolean value) {
		if (unrestricted.isEnabled()) {
			unrestricted.setValue(value, true);
		}
	}

	@Override
	public Widget asWidget() {
		return form;
	}

	@Override
	public void setTitleText(String s) {
	}

	@Override
	public String getStringValue() {
		return null;
	}

	@Override
	public void setStringValue(String v) {
	}

	@Override
	public void setPropertyName(String string) {
	}

	@Override
	public String getPropertyName() {
		return null;
	}

	@UiFactory
	IdentityConstants getTexts() {
		return IdentityConstants.INST;
	}

	@Override
	public void setReadOnly(boolean readOnly) {
	}
}
