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

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;
import net.bluemind.ui.gwtuser.client.UserBooksSubscriptionModelHandler;
import net.bluemind.ui.mailbox.filter.MailForwardEditor;
import net.bluemind.ui.mailbox.filter.MailSettingsModelHandler;
import net.bluemind.ui.mailbox.filter.SieveEdit;
import net.bluemind.ui.mailbox.identity.UserIdentityManagement;
import net.bluemind.ui.mailbox.identity.UserMailIdentitiesModelHandler;
import net.bluemind.ui.mailbox.sharing.MailboxesSharingsEditor;
import net.bluemind.ui.mailbox.sharing.MailboxesSharingsModelHandler;
import net.bluemind.ui.mailbox.vacation.MailVacationEditor;

public class MailSettingsPlugin {

	public static void install() {

		MenuContributor.exportAsfunction("gwtSettingsMailMenusContributor",
				MenuContributor.create(new MailMenusContributor()));

		ScreenElementContributor.exportAsfunction("gwtSettingsMailScreensContributor",
				ScreenElementContributor.create(new MailScreensContributor()));

		SieveEdit.registerType();
		MailVacationEditor.registerType();

		AdvancedLink.registerType();
		UserIdentityManagement.registerType();

		MailboxesSharingsEditor.registerType();

		MailSettingsModelHandler.registerType();
		UserMailIdentitiesModelHandler.registerType();
		UserBooksSubscriptionModelHandler.registerType();
		MailboxesSharingsModelHandler.registerType();
		MailForwardEditor.registerType();
		
		NewWebmailSettings.registerType();

	}
}
