/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
import com.google.gwt.editor.client.IsEditor;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.api.gwt.js.JsEmail;
import net.bluemind.domain.api.Domain;

public class MailAddress extends Composite
		implements HasValueChangeHandlers<JsEmail>, IsEditor<LeafValueEditor<JsEmail>> {

	private final List<ValueChangeHandler<JsEmail>> valueChangeHandlers = new ArrayList<ValueChangeHandler<JsEmail>>();

	private static final String EMAIL = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(([a-z0-9-]+\\.)+[a-z]{2,}|all)$";
	public static final RegExp emailPattern = RegExp.compile(EMAIL);

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

		String invalid();

		String mailContainer();

		String icon();

		String prependedText();
	}

	public static interface MailAddressConstants extends Constants {
		String invalidEmail();

		String allAliases();

		String addEmail();

		String removeEmail();

	}

	private static final MailAddressConstants constants = GWT.create(MailAddressConstants.class);

	private static final Resources RES = GWT.create(Resources.class);

	TextBox textBox = new TextBox();
	ListBox listBox = new ListBox();

	private Label invalidIcon = new Label();
	private Label addOrRemoveIcon = new Label();
	private Domain domain;
	boolean userMailbox = false;

	public MailAddress(int pos, Domain d, boolean userMailbox, String login) {
		domain = d;
		this.userMailbox = userMailbox;
		fillAliasesListBox();
		initMailAddressWidget(pos);
		// Initialize "initial" login part, set from MailAdressTable
		textBox.setText(login);
		this.addValueChangeHandler(evt -> {
			JsEmail jsemail = evt.getValue();
			invalidIcon.setVisible(!isValid(jsemail.getAddress()));
		});
	}

	private void initMailAddressWidget(int pos) {
		Style style = RES.mailAddressStyle();
		style.ensureInjected();

		FlowPanel panel = new FlowPanel();
		panel.setStyleName(style.mailContainer());

		textBox.addStyleName(style.textBox());
		listBox.addStyleName(style.listBox());
		Label at = new Label("@");
		at.addStyleName(style.at());

		panel.add(invalidIcon);
		panel.add(textBox);
		panel.add(at);
		panel.add(listBox);
		panel.add(addOrRemoveIcon);

		invalidIcon.addStyleName(style.invalid());
		invalidIcon.addStyleName("fa fa-lg fa-exclamation-triangle");
		invalidIcon.setVisible(false);
		invalidIcon.setTitle(constants.invalidEmail());

		addOrRemoveIcon.setStyleName("fa fa-lg fa-minus-square-o");
		addOrRemoveIcon.setTitle(constants.removeEmail());

		textBox.addChangeHandler(evt -> fireChangeEvent());
		listBox.addChangeHandler(evt -> fireChangeEvent());

		initWidget(panel);
	}

	public static boolean isValid(String emailAddress) {
		return emailPattern.test(emailAddress);
	}

	public Label getImage() {
		return addOrRemoveIcon;
	}

	public void setIconAdd() {
		addOrRemoveIcon.setStyleName("alias-icon fa fa-lg fa-plus-square-o");
		addOrRemoveIcon.setTitle(constants.addEmail());
	}

	public void setIconRemove() {
		addOrRemoveIcon.setStyleName("alias-icon fa fa-lg fa-minus-square-o");
		addOrRemoveIcon.setTitle(constants.removeEmail());
	}

	private void fillAliasesListBox() {
		listBox.clear();
		// Only use mailbox have "all" aliases option
		if (userMailbox) {
			listBox.addItem(constants.allAliases(), "all");
		}

		if (domain.aliases != null) {
			for (String alias : domain.aliases) {
				listBox.addItem(alias, alias);
			}
		}
	}

	private LeafValueEditor<JsEmail> editor = new LeafValueEditor<JsEmail>() {
		@Override
		public void setValue(JsEmail value) {
			String[] mail = value.getAddress().split("@");
			textBox.setValue(mail[0]);
			if (value.getAllAliases()) {
				listBox.setSelectedIndex(0);
			} else {
				selectDomain(mail[1]);
			}
			invalidIcon.setVisible(!emailPattern.test(value.getAddress()));
		}

		@Override
		public JsEmail getValue() {
			String left = "";
			if (textBox.getValue() != null && !textBox.getValue().isEmpty()) {
				left = textBox.getValue();
			}
			String domainName = listBox.getSelectedValue();
			JsEmail ret = JsEmail.create();
			ret.setAddress(left + "@" + domainName);
			ret.setIsDefault(false);
			// Only user mailbox have an "all" aliases option
			if (userMailbox) {
				ret.setAllAliases("all".equals(domainName));
			} else {
				ret.setAllAliases(false);
			}
			return ret;
		}

		private void selectDomain(String domainName) {
			for (int i = 0; i < listBox.getItemCount(); i++) {
				if (listBox.getValue(i).equals(domainName)) {
					listBox.setSelectedIndex(i);
					break;
				}
			}
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

	private void fireChangeEvent() {
		ValueChangeEvent.fire(MailAddress.this, asEditor().getValue());
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
}
