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
package net.bluemind.ui.adminconsole.directory.externaluser;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.addressbook.api.gwt.js.JsVCard;
import net.bluemind.addressbook.api.gwt.js.JsVCardIdentification;
import net.bluemind.addressbook.api.gwt.js.JsVCardIdentificationName;
import net.bluemind.core.api.gwt.js.JsEmail;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.js.JsItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.js.JsDomain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.externaluser.api.gwt.js.JsExternalUser;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.ui.DelegationEdit;
import net.bluemind.ui.adminconsole.directory.externaluser.l10n.ExternalUserConstants;
import net.bluemind.ui.common.client.errors.ErrorCodeTexts;

public class NewExternalUser extends CompositeGwtWidgetElement {

	interface NewExternalUserUiBinder
			extends UiBinder<HTMLPanel, NewExternalUser> {
	}

	public static final String TYPE = "bm.ac.QCreateExternalUserWidget";

	private static NewExternalUserUiBinder uiBinder = GWT
			.create(NewExternalUserUiBinder.class);

	private ItemValue<Domain> domain;

	private HTMLPanel dlp;

	@UiField
	DelegationEdit delegation;

	@UiField
	TextBox firstName;

	@UiField
	TextBox lastName;

	@UiField
	TextBox email;

	@UiField
	Label errorLabel;

	@UiField
	CheckBox hidden;

	private ItemValue<Domain> externalUserDomain;

	private NewExternalUser() {
		dlp = uiBinder.createAndBindUi(this);
		initWidget(dlp);
		firstName.getElement().setId("new-externaluser-firstname");
		lastName.getElement().setId("new-externaluser-lastname");
		email.getElement().setId("new-externaluser-email");

		dlp.setHeight("100%");
		this.externalUserDomain = DomainsHolder.get().getSelectedDomain();
		updateDomainChange(externalUserDomain);
	}

	private void updateDomainChange(ItemValue<Domain> d) {
		this.domain = d;
		delegation.setDomain(d.uid);
		if (domain.value.global) {
			errorLabel.setText(ErrorCodeTexts.INST.getString("NOT_IN_GLOBAL_DOMAIN"));
		} else {
			errorLabel.setText("");
		}
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();

		if (map.get("domain") != null) {
			JsItemValue<JsDomain> domain = map.get("domain").cast();

			ItemValue<Domain> d = new ItemValueGwtSerDer<>(new DomainGwtSerDer()).deserialize(new JSONObject(domain));
			updateDomainChange(d);
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		JsMapStringJsObject map = model.cast();
		JsExternalUser externalUser = map.get("externaluser").cast();

		externalUser.setOrgUnitUid(delegation.asEditor().getValue());
		externalUser.setHidden(hidden.asEditor().getValue());

		JsVCard vcard = JsVCard.create();
		vcard.setIdentification(JsVCardIdentification.create());
		vcard.getIdentification().setPhoto(false);
		JsVCardIdentificationName name = JsVCardIdentificationName.create();
		vcard.getIdentification().setName(name);
		externalUser.setContactInfos(vcard);
		name.setFamilyNames(lastName.getText());
		name.setGivenNames(firstName.getText());

		JsArray<JsEmail> emails = JsArray.createArray().cast();
		JsEmail euEmail = JsEmail.create();
		euEmail.setAddress(email.asEditor().getValue());
		euEmail.setIsDefault(true);
		euEmail.setAllAliases(false);
		emails.push(euEmail);
		externalUser.setEmails(emails);
	}

	@UiFactory
	ExternalUserConstants getConstants() {
		return ExternalUserConstants.INST;
	}

	public static void registerType() {
		GwtWidgetElement.register("bm.ac.QCreateExternalUserWidget",
				new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

					@Override
					public IGwtWidgetElement create(WidgetElement e) {
						return new NewExternalUser();
					}
				});
		GWT.log("bm.ac.QCreateExternalUserWidget registred");
	}
}
