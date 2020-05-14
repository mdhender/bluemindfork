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
package net.bluemind.ui.settings.mail;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayUtils;

import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.gwtconsoleapp.base.editor.TabContainer;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwtuser.client.MailboxSubscriptionsEditor;
import net.bluemind.ui.gwtuser.client.MailboxSubscriptionsModelHandler;
import net.bluemind.ui.mailbox.filter.DomainFilters;
import net.bluemind.ui.mailbox.filter.MailForwardEditor;
import net.bluemind.ui.mailbox.filter.MailSettingsModelHandler;
import net.bluemind.ui.mailbox.filter.SieveEdit;
import net.bluemind.ui.mailbox.identity.UserIdentityManagement;
import net.bluemind.ui.mailbox.identity.UserMailIdentitiesModelHandler;
import net.bluemind.ui.mailbox.sharing.MailboxesSharingsEditor;
import net.bluemind.ui.mailbox.sharing.MailboxesSharingsModelHandler;
import net.bluemind.ui.mailbox.vacation.MailVacationEditor;

public class MailScreensContributor implements ScreenElementContributorUnwrapper {
	private static final MailMessages messages = GWT.create(MailMessages.class);

	@Override
	public JsArray<ScreenElementContribution> contribution() {

		DomainFilters.registerType();
		JsArray<ScreenElement> generalEtls = JsArray.createArray().cast();
		generalEtls.push(ScreenElement.create(null, MailVacationEditor.TYPE));
		generalEtls.push(ScreenElement.create(null, MailForwardEditor.TYPE).withRole(BasicRoles.ROLE_MAIL_FORWARDING));
		generalEtls.push(ScreenElement.create(null, AdvancedLink.TYPE));
		ContainerElement userGeneralContainer = ContainerElement.create("mailGenralContainer", generalEtls);

		JsArray<Tab> tabs = JsArray.createArray().cast();
		tabs.push(Tab.create(null, messages.tabGeneral(), userGeneralContainer));

		ScreenElement mailboxFilter = ScreenElement.create(null, SieveEdit.TYPE)
				.withRole(BasicRoles.ROLE_SELF_CHANGE_MAILBOX_FILTER);
		ScreenElement domainFilter = ScreenElement.create(null, DomainFilters.TYPE)
				.withRole(BasicRoles.ROLE_READ_DOMAIN_FILTER);

		ContainerElement filtersContainer = ContainerElement.create("filtersContainer",
				JsArrayUtils.readOnlyJsArray(new ScreenElement[] { domainFilter, mailboxFilter }));
		tabs.push(Tab.create(null, messages.tabFilters(), filtersContainer));
		tabs.push(Tab.create(null, messages.tabIdentities(), ScreenElement.create(null, UserIdentityManagement.TYPE)
				.withRole(BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES)));

		ScreenElement contribution = TabContainer.create("/webmail/", tabs);
		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();
		contribs.push(ScreenElementContribution.create("root", "childrens", contribution));

		contribs.push(ScreenElementContribution.create("base", "modelHandlers", ScreenElement
				.create(null, MailSettingsModelHandler.TYPE).withRole(BasicRoles.ROLE_SELF_CHANGE_MAILBOX_FILTER)));
		contribs.push(ScreenElementContribution.create("base", "modelHandlers",
				ScreenElement.create(null, UserMailIdentitiesModelHandler.TYPE)
						.withRole(BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES)));

		if ("FULL".equals(Ajax.getAccountType())) {
			tabs.push(Tab.create(null, messages.tabSubscriptions(),
					ScreenElement.create(null, MailboxSubscriptionsEditor.TYPE)));
			contribs.push(ScreenElementContribution.create("base", "modelHandlers",
					ScreenElement.create(null, MailboxSubscriptionsModelHandler.TYPE)));

			tabs.push(
					Tab.create(null, messages.tabSharings(), ScreenElement.create(null, MailboxesSharingsEditor.TYPE)));
			contribs.push(ScreenElementContribution.create("base", "modelHandlers",
					ScreenElement.create(null, MailboxesSharingsModelHandler.TYPE)));
		}

		if (Ajax.TOKEN.getRoles().contains("hasMailWebapp")) {
			ScreenElement newWebmailSettings = ScreenElement.create(null, NewWebmailSettings.TYPE);
			tabs.push(Tab.create(null, messages.mailApp(), newWebmailSettings));
		}

		return contribs;

	}

}
