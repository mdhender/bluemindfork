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
package net.bluemind.ui.adminconsole.base.ui;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.api.gwt.js.JsEmail;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class MailAddress extends Composite
		implements HasValueChangeHandlers<JsEmail>, IsEditor<LeafValueEditor<JsEmail>> {

	private final List<ValueChangeHandler<JsEmail>> valueChangeHandlers = new ArrayList<ValueChangeHandler<JsEmail>>();

	public static interface Resources extends ClientBundle {
		@Source("MailAddress.css")
		Style mailAddressStyle();
	}

	public static interface Style extends CssResource {
		String emailPanel();

		String textBox();

		String listBox();

		String defaultListBox();

		String defaultListBoxLine();

		String at();

		String mailContainer();

		String icon();

		String prependedText();
	}

	public static interface MailAddressConstants extends Constants {
		String allAliases();

		String addEmail();

		String removeEmail();

	}

	private static final MailAddressConstants constants = GWT.create(MailAddressConstants.class);

	private static final Resources RES = GWT.create(Resources.class);

	private Style s;

	TextBox textBox = new TextBox();
	ListBox listBox = new ListBox();
	private Label addOrRemoveIcon = new Label();
	private Domain domain;
	boolean implicit = false;

	public MailAddress() {
		addOrRemoveIcon.setStyleName("fa fa-lg fa-minus-square-o");
	}

	public MailAddress(Domain d, int pos) {
		this();
		domain = d;

		initMailAddress(pos);
		fillAliasesListBox(domain.name);
	}

	public MailAddress(int pos) {
		this();
		initMailAddress(pos);
	}

	private void initMailAddress(int pos) {
		s = RES.mailAddressStyle();
		s.ensureInjected();

		FlowPanel panel = new FlowPanel();
		panel.setStyleName(s.mailContainer());

		textBox.addStyleName(s.textBox());
		textBox.getElement().setId("mail-" + pos);
		listBox.addStyleName(s.listBox());
		Label at = new Label("@");
		at.addStyleName(s.at());

		panel.add(textBox);
		panel.add(at);
		panel.add(listBox);
		panel.add(addOrRemoveIcon);

		addOrRemoveIcon.setStyleName(s.icon());
		addOrRemoveIcon.setTitle(constants.removeEmail());

		textBox.getElement().setId("mail-alias-localpart-" + pos);
		textBox.addChangeHandler(evt -> ValueChangeEvent.fire(MailAddress.this, asEditor().getValue()));

		listBox.getElement().setId("mail-alias-domainpart-" + pos);
		listBox.addChangeHandler(evt -> ValueChangeEvent.fire(MailAddress.this, asEditor().getValue()));

		initWidget(panel);
	}

	public Label getImage() {
		return addOrRemoveIcon;
	}

	public void setFirst() {
		addOrRemoveIcon.setStyleName("alias-icon fa fa-lg fa-plus-square-o");
		addOrRemoveIcon.setTitle(constants.addEmail());
	}

	public void setFollowing() {
		addOrRemoveIcon.setStyleName("alias-icon fa fa-lg fa-minus-square-o");
		addOrRemoveIcon.setTitle(constants.removeEmail());
	}

	public void setDomain(ItemValue<Domain> d) {
		if (d == null) {
			throw new IllegalArgumentException("should not be null..");
		}
		domain = d.value;
		fillAliasesListBox(domain.name);
	}

	private void fillAliasesListBox(String domainName) {
		listBox.clear();
		if (!implicit) {
			listBox.addItem(constants.allAliases(), "all");
		}

		listBox.addItem(domain.name, domain.name);
		if (!implicit && domain.aliases != null) {
			for (String alias : domain.aliases) {
				listBox.addItem(alias, alias);
			}
		}

		selectDomainBox(domainName);
	}

	private void selectDomainBox(String domainName) {
		int count = listBox.getItemCount();
		for (int i = 0; i < count; i++) {
			if (listBox.getValue(i).equals(domainName)) {
				listBox.setSelectedIndex(i);
				break;
			}
		}
	}

	private LeafValueEditor<JsEmail> editor = new LeafValueEditor<JsEmail>() {

		@Override
		public void setValue(JsEmail value) {
			String[] mail = value.getAddress().split("@");
			if (mail.length != 2) {
				throw new RuntimeException("Invalid email without 2 parts: " + value);
			}
			textBox.setValue(mail[0]);
			if (value.getAllAliases()) {
				listBox.setSelectedIndex(0);
			} else {
				selectDomainBox(mail[1]);
			}

		}

		@Override
		public JsEmail getValue() {
			String left = "";
			if (textBox.getValue() != null && !textBox.getValue().isEmpty()) {
				left = textBox.getValue();
			}

			JsEmail ret = JavaScriptObject.createObject().cast();
			ret.setIsDefault(false);
			ret.setAllAliases(false);
			if (listBox.getSelectedIndex() == 0) {
				ret.setAllAliases(true);
				ret.setAddress(left + "@" + domain.name);
			} else {
				ret.setAddress(left + "@" + listBox.getSelectedValue());
			}
			return ret;
		}
	};

	@Override
	public LeafValueEditor<JsEmail> asEditor() {
		return editor;
	}

	@Override
	public HandlerRegistration addValueChangeHandler(final ValueChangeHandler<JsEmail> handler) {
		valueChangeHandlers.add(handler);
		return new HandlerRegistration() {

			@Override
			public void removeHandler() {
				valueChangeHandlers.remove(handler);

			}
		};
	}

	public void setImplicit() {
		implicit = true;
		textBox.setEnabled(false);
		listBox.setEnabled(false);
	}

	public void fireEvent(GwtEvent<?> event) {
		if (event instanceof ValueChangeEvent) {
			for (ValueChangeHandler<JsEmail> handler : valueChangeHandlers) {
				handler.onValueChange((ValueChangeEvent<JsEmail>) event);
			}
		} else {
			super.fireEvent(event);
		}
	}

	public static String getAddress(JsEmail jsEmail) {
		if (jsEmail.getAllAliases()) {
			return jsEmail.getAddress().split("@")[0] + "@all";
		} else {
			return jsEmail.getAddress();
		}
	}
}
