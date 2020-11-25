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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
	private boolean hasImplicitEmail;

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

	public static interface MailAddressTableConstants extends Constants {
		String defaultEmail();
	}

	private static final MailAddressTableConstants constants = GWT.create(MailAddressTableConstants.class);

	private static final Resources RES = GWT.create(Resources.class);
	private Style s;

	public static interface MailAddressConstants extends Constants {
		String allAliases();
	}

	@UiConstructor
	public MailAddressTable(int size, boolean hasImplicitEmail) {
		s = RES.mailAddressStyle();
		s.ensureInjected();

		this.hasImplicitEmail = hasImplicitEmail;
		sizeLimit = size == -1 ? Integer.MAX_VALUE : size;
		emailPanel = new VerticalPanel();
		emailPanel.setStyleName(s.emailPanel());
		initWidget(emailPanel);

		mailAdressList = new ArrayList<>();

		initPanel();
	}

	private void initPanel() {
		FlowPanel emailDefaultPanel = new FlowPanel();
		emailDefaultPanel.setStyleName(s.defaultListBoxLine());
		Label prependedText = new Label();
		prependedText.setText(constants.defaultEmail());
		prependedText.setStyleName(s.prependedText());
		emailDefaultPanel.add(prependedText);

		defaultEmailList = new MailAddressDefault();
		defaultEmailList.addStyleName(s.defaultListBox());
		emailDefaultPanel.add(defaultEmailList);
		emailPanel.add(emailDefaultPanel);
	}

	public void setReadOnly(boolean readOnly) {
		// FIXME
	}

	private MailAddress createNewAddressElement(int pos) {
		final MailAddress ma = new MailAddress(mailAdressList.size());
		ma.setDomain(domain);
		ma.addValueChangeHandler(evt -> updateDefaultAddressList(null));
		mailAdressList.add(ma);
		emailPanel.add(ma);

		if (pos == 0) {
			ma.setFirst();
			if (sizeLimit > 1) {
				mailAdressList.get(0).getImage().addClickHandler(evt -> {
					if (mailAdressList.size() < sizeLimit) {
						createNewAddressElement(mailAdressList.size());
					}

					if (mailAdressList.size() == sizeLimit) {
						mailAdressList.get(0).getImage().setVisible(false);
					}
				});
			} else {
				mailAdressList.get(0).getImage().setVisible(false);
			}

			if (hasImplicitEmail) {
				ma.setImplicit();
			}

		} else {
			ma.setFollowing();
			ma.getImage().addClickHandler(evt -> {
				emailPanel.remove(ma);
				mailAdressList.remove(ma);
				mailAdressList.get(0).getImage().setVisible(true);
				updateDefaultAddressList(null);
			});
		}
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
		domain = d;
		for (MailAddress ma : mailAdressList) {
			ma.setDomain(d);
		}
	}

	public JsArray<JsEmail> getValue() {
		JsArray<JsEmail> mapMail = JsArray.createArray().cast();
		Map<String, Integer> allAdded = new HashMap<>();

		List<JsEmail> emails = mailAdressList.stream().map(ma -> ma.asEditor().getValue()).filter(Objects::nonNull)
				.collect(Collectors.toList());
		String defaultMail = defaultEmailList.getSelectedValue();

		for (JsEmail email : emails) {
			String address = email.getAddress();
			String[] split = address.split("@");
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
			if (allAdded.containsKey(split[0])) {
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

		if (mailAdressList.isEmpty()) {
			createNewAddressElement(0);
		}

		mailAdressList.get(0).asEditor().setValue(emails.get(0));

		String defaultEmail = null;
		if (emails.get(0).getIsDefault()) {
			defaultEmail = emails.get(0).getAddress();
		}
		for (int i = 1; i < emails.length(); i++) {
			JsEmail email = emails.get(i);
			if (email.getIsDefault()) {
				defaultEmail = email.getAddress();
			}
			MailAddress ma = createNewAddressElement(i);
			ma.asEditor().setValue(email);
		}
		updateDefaultAddressList(defaultEmail);
	}

	private List<String> expandMail(JsEmail email) {
		List<String> ret = new ArrayList<>();

		String leftPart = email.getAddress().split("@")[0];
		ret.add(leftPart + "@" + domain.value.name);

		for (String al : domain.value.aliases) {
			ret.add(leftPart + "@" + al);
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

	public void setValue(String login, String domain) {
		String email = login + "@" + domain;
		reset();
		MailAddress ma = createNewAddressElement(0);
		JsEmail jemail = JavaScriptObject.createObject().cast();
		jemail.setAddress(email);
		jemail.setAllAliases(false);
		jemail.setIsDefault(false);
		ma.asEditor().setValue(jemail);
		updateDefaultAddressList(email);
	}

	public void setSingleValue(String login, String domain) {
		hasImplicitEmail = true;
		setValue(login, domain);
		mailAdressList.get(0).implicit = false;
		mailAdressList.get(0).textBox.setEnabled(true);
		mailAdressList.get(0).listBox.setEnabled(true);
	}
}
