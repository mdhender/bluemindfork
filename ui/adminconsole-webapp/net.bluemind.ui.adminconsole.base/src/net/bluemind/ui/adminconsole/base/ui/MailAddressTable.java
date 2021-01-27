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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import net.bluemind.core.api.gwt.js.JsEmail;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;

public class MailAddressTable extends Composite {

	private int sizeLimit = 5;
	private List<MailAddress> mailAdressList;
	private MailAddressDefault defaultEmailList;
	private VerticalPanel emailPanel;
	private ItemValue<Domain> domain;
	private boolean isUserMailbox;
	private String defaultLogin;

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

		String invalid();
	}

	public static interface MailAddressTableConstants extends Constants {
		String defaultEmail();
	}

	private static final MailAddressTableConstants constants = GWT.create(MailAddressTableConstants.class);

	private static final Resources RES = GWT.create(Resources.class);
	private Style style;

	public static interface MailAddressConstants extends Constants {
		String allAliases();
	}

	@UiConstructor
	public MailAddressTable(int size, boolean isUserMailbox) {
		style = RES.mailAddressStyle();
		style.ensureInjected();

		this.isUserMailbox = isUserMailbox;
		sizeLimit = size == -1 ? Integer.MAX_VALUE : size;
		emailPanel = new VerticalPanel();
		emailPanel.setStyleName(style.emailPanel());
		initWidget(emailPanel);

		mailAdressList = new ArrayList<>();
		initPanel();
	}

	private void initPanel() {
		FlowPanel emailDefaultPanel = new FlowPanel();
		emailDefaultPanel.setStyleName(style.defaultListBoxLine());
		Label prependedText = new Label();
		prependedText.setText(constants.defaultEmail());
		prependedText.setStyleName(style.prependedText());
		emailDefaultPanel.add(prependedText);

		defaultEmailList = new MailAddressDefault();
		defaultEmailList.addStyleName(style.defaultListBox());
		emailDefaultPanel.add(defaultEmailList);
		emailPanel.add(emailDefaultPanel);
	}

	public void setReadOnly(boolean enabled) {
		// FIXME
	}

	private MailAddress createNewAddressElement() {
		final int pos = mailAdressList.size();
		final MailAddress ma = new MailAddress(mailAdressList.size(), domain.value, isUserMailbox, defaultLogin);

		mailAdressList.add(ma);
		emailPanel.add(ma);

		if (pos == 0) {
			ma.setIconAdd();
			if (sizeLimit > 1) {
				mailAdressList.get(0).getImage().addClickHandler(evt -> {
					if (mailAdressList.size() < sizeLimit) {
						createNewAddressElement();
					}
				});
			}
			if (mailAdressList.size() >= sizeLimit) {
				mailAdressList.get(0).getImage().setVisible(false);
			}
		} else {
			ma.setIconRemove();
			ma.getImage().addClickHandler(evt -> {
				emailPanel.remove(ma);
				mailAdressList.remove(ma);
				mailAdressList.get(0).getImage().setVisible(true);
				updateDefaultAddressList(null);
			});
		}

		ma.addValueChangeHandler(evt -> updateDefaultAddressList(null));

		return ma;
	}

	protected void updateDefaultAddressList(String defaultEmail) {
		Set<String> emails = new HashSet<>();
		for (MailAddress ma : mailAdressList) {
			JsEmail email = ma.asEditor().getValue();
			if (email != null) {
				if (email.getAllAliases()) {
					emails.addAll(expandMail(email));
				} else {
					emails.add(email.getAddress());
				}
			}
		}
		defaultEmailList.updateDefaultAddressList(emails, defaultEmail);
	}

	public void setDomain(ItemValue<Domain> d) {
		reset();
		domain = d;
	}

	public JsArray<JsEmail> getValue() {
		JsArray<JsEmail> mapMail = JsArray.createArray().cast();
		Map<String, Integer> allAdded = new HashMap<>();

		List<JsEmail> emails = mailAdressList.stream().map(ma -> ma.asEditor().getValue()).filter(Objects::nonNull)
				.filter(jsmail -> MailAddress.isValid(jsmail.getAddress())).collect(Collectors.toList());
		String defaultMail = defaultEmailList.getSelectedValue();

		for (JsEmail email : emails) {
			String address = email.getAddress();
			String[] split = address.split("@");

			// Empty login
			if (split[0].isEmpty()) {
				continue;
			}
			if (email.getAllAliases()) {
				List<String> xemails = expandMail(email);
				JsEmail mail;
				if (xemails.contains(defaultMail)) {
					mail = asJsEmail(defaultMail, true, true);
				} else {
					mail = asJsEmail(split[0] + "@" + domain.value.name, false, true);
				}
				int index = -1;
				for (int i = 0; i < mapMail.length(); i++) {
					if (mapMail.get(i).getAddress().equals(mail.getAddress())) {
						index = i;
					}
				}
				if (index > -1) {
					allAdded.put(split[0], index);
					mapMail.set(index, mail);
				} else {
					mapMail.push(mail);
					allAdded.put(split[0], mapMail.length() - 1);
				}
			}
		}
		for (JsEmail email : emails) {
			String address = email.getAddress();
			String[] split = address.split("@");
			if (allAdded.containsKey(split[0]) || split[0].isEmpty()) {
				if (defaultMail.equals(address)) {
					JsEmail jsEmail = mapMail.get(allAdded.get(split[0]));
					jsEmail.setAddress(address);
					jsEmail.setIsDefault(true);
				}
				continue;
			}
			if (!email.getAllAliases()) {
				int index = -1;
				for (int i = 0; i < mapMail.length(); i++) {
					if (mapMail.get(i).getAddress().equals(address)) {
						index = i;
					}
				}
				if (index == -1) {
					JsEmail emai2l = asJsEmail(address, defaultMail.equals(address), false);
					mapMail.push(emai2l);
				}
			}

		}

		return mapMail;
	}

	void setValue(JsArray<JsEmail> emails) {
		reset();
		if (emails.length() == 0) {
			return;
		}

		List<JsEmail> filteredEmails = new ArrayList<>(emails.length());
		for (int i = 0; i < emails.length(); i++) {
			JsEmail email = emails.get(i);
			String[] mailpart = email.getAddress().split("@");
			if (!email.getAllAliases() && !domain.value.aliases.contains(mailpart[1])) {
				// this is the implicit email address (@domain.uid)
				GWT.log("ignore implicit email " + email.getAddress());
			} else {
				filteredEmails.add(email);
			}
		}

		// This is the logic for adding an empty element to the list
		// if we want to create a new user, without an email address
		// or we change the routing from None/External to Internal.
		if (mailAdressList.isEmpty()) {
			createNewAddressElement();
		}

		if (!filteredEmails.isEmpty()) {
			String defaultEmail = filteredEmails.stream().filter(JsEmail::getIsDefault).map(JsEmail::getAddress)
					.findAny().orElse(null);
			JsEmail firstEmail = filteredEmails.get(0);
			mailAdressList.get(0).asEditor().setValue(firstEmail);
			for (int i = 1; i < filteredEmails.size(); i++) {
				createNewAddressElement().asEditor().setValue(filteredEmails.get(i));
			}
			updateDefaultAddressList(defaultEmail);
		}
	}

	private List<String> expandMail(JsEmail email) {
		List<String> ret = new ArrayList<>();
		String leftPart = email.getAddress().split("@")[0];
		ret.add(leftPart + "@" + domain.value.defaultAlias);
		for (String alias : domain.value.aliases) {
			ret.add(leftPart + "@" + alias);
		}
		return ret;
	}

	private JsEmail asJsEmail(String address, boolean isDefault, boolean allAliases) {
		JsEmail ret = JsEmail.create();
		ret.setAddress(address);
		ret.setIsDefault(isDefault);
		ret.setAllAliases(allAliases);
		return ret;
	}

	public void reset() {
		emailPanel.clear();
		mailAdressList.clear();
		initPanel();
	}

	public static native String unaccent(String str)
	/*-{
    return str.normalize('NFKD').replace(/[\u0300-\u036f]/g, "");
	}-*/;

	private static String sanitizeLoginForEmail(final String login) {
		String newname = login;
		if (login != null && !login.isEmpty()) {
			newname = unaccent(login.toLowerCase()).replaceAll("[^\\.a-z0-9!#$%&'*+/=?^_`{|}~-]", "");
		}
		return newname;
	}

	public void setValue(String login, String domain) {
		boolean isDefaultAlias = false;
		if ("all".equals(domain)) {
			domain = this.domain.value.defaultAlias;
			isDefaultAlias = true;
		}
		String email = sanitizeLoginForEmail(login) + "@" + domain;
		JsArray<JsEmail> emails = JavaScriptObject.createArray().cast();
		emails.push(asJsEmail(email, true, isDefaultAlias));
		setValue(emails);
	}

	public void setDefaultLogin(String login) {
		defaultLogin = sanitizeLoginForEmail(login);
	}
}
