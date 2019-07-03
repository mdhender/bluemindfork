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
package net.bluemind.ui.adminconsole.system;

import com.google.gwt.core.client.GWT;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributor;
import net.bluemind.gwtconsoleapp.base.menus.MenuContributor;
import net.bluemind.ui.adminconsole.system.domains.DomainsScreen;
import net.bluemind.ui.adminconsole.system.domains.create.QCreateDomainModelHandler;
import net.bluemind.ui.adminconsole.system.domains.create.QCreateDomainScreen;
import net.bluemind.ui.adminconsole.system.domains.create.QCreateDomainWidget;
import net.bluemind.ui.adminconsole.system.domains.edit.DomainAssignmentsModelHandler;
import net.bluemind.ui.adminconsole.system.domains.edit.DomainModelHandler;
import net.bluemind.ui.adminconsole.system.domains.edit.DomainSettingsModelHandler;
import net.bluemind.ui.adminconsole.system.domains.edit.EditDomainScreen;
import net.bluemind.ui.adminconsole.system.domains.edit.ServersModelHandler;
import net.bluemind.ui.adminconsole.system.domains.edit.bmservices.EditDomainBmServicesEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.filters.EditDomainFiltersEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.filters.FiltersModelHandler;
import net.bluemind.ui.adminconsole.system.domains.edit.general.DomainMaxBasicAccountEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.general.DomainMaxUserEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.general.EditDomainGeneralEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.indexing.EditDomainIndexingEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.instantmessaging.EditDomainInstantMessagingEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.instantmessaging.ImModelHandler;
import net.bluemind.ui.adminconsole.system.domains.edit.mailflow.EditMailflowRulesEditor;
import net.bluemind.ui.adminconsole.system.domains.edit.mailsystem.EditDomainMailsystemEditor;
import net.bluemind.ui.adminconsole.system.hosts.HostsScreen;
import net.bluemind.ui.adminconsole.system.hosts.create.QCreateHostModelHandler;
import net.bluemind.ui.adminconsole.system.hosts.create.QCreateHostScreen;
import net.bluemind.ui.adminconsole.system.hosts.create.QCreateHostWidget;
import net.bluemind.ui.adminconsole.system.hosts.edit.DomainTemplateModelHandler;
import net.bluemind.ui.adminconsole.system.hosts.edit.EditHostBasicEditor;
import net.bluemind.ui.adminconsole.system.hosts.edit.EditHostScreen;
import net.bluemind.ui.adminconsole.system.hosts.edit.EditHostServerRolesEditor;
import net.bluemind.ui.adminconsole.system.hosts.edit.ServerModelHandler;
import net.bluemind.ui.adminconsole.system.hosts.edit.UserLanguageModelHandler;
import net.bluemind.ui.adminconsole.system.maintenance.MaintenanceScreen;
import net.bluemind.ui.adminconsole.system.subscription.SubscriptionModelHandler;
import net.bluemind.ui.adminconsole.system.subscription.SubscriptionWidget;
import net.bluemind.ui.adminconsole.system.systemconf.GlobalSettingsModelHandler;
import net.bluemind.ui.adminconsole.system.systemconf.SysConfModelHandler;
import net.bluemind.ui.adminconsole.system.systemconf.SystemConfScreen;
import net.bluemind.ui.adminconsole.system.systemconf.auth.SysConfAuthenticationEditor;
import net.bluemind.ui.adminconsole.system.systemconf.eas.SysConfEasServerEditor;
import net.bluemind.ui.adminconsole.system.systemconf.mail.SysConfMailEditor;
import net.bluemind.ui.adminconsole.system.systemconf.reverseProxy.SysConfReverseProxyEditor;
import net.bluemind.ui.gwttag.client.DomainTagsEditor;
import net.bluemind.ui.gwttag.client.DomainTagsModelHandler;

public class SystemPlugin {

	public static void init() {
		GWT.log("Init System plugin");
		// System configuration
		SysConfModelHandler.registerType();
		GlobalSettingsModelHandler.registerType();
		SystemConfScreen.registerType();
		SysConfMailEditor.registerType();
		SysConfReverseProxyEditor.registerType();
		SysConfEasServerEditor.registerType();
		SysConfAuthenticationEditor.registerType();

		// Application servers (Hosts)
		HostsScreen.registerType();
		DomainTemplateModelHandler.registerType();
		UserLanguageModelHandler.registerType();
		ServerModelHandler.registerType();
		EditHostScreen.registerType();
		EditHostBasicEditor.registerType();
		EditHostServerRolesEditor.registerType();
		QCreateHostModelHandler.registerType();
		QCreateHostScreen.registerType();
		QCreateHostWidget.registerType();

		// Domains
		DomainModelHandler.registerType();
		QCreateDomainModelHandler.registerType();
		QCreateDomainScreen.registerType();
		QCreateDomainWidget.registerType();
		EditDomainScreen.registerType();
		DomainsScreen.registerType();
		EditDomainBmServicesEditor.registerType();
		EditDomainFiltersEditor.registerType();
		EditDomainGeneralEditor.registerType();
		DomainMaxUserEditor.registerType();
		DomainMaxBasicAccountEditor.registerType();
		EditDomainIndexingEditor.registerType();
		EditDomainInstantMessagingEditor.registerType();
		EditDomainMailsystemEditor.registerType();
		DomainTagsModelHandler.registerType();
		DomainTagsEditor.registerType();
		DomainModelHandler.registerType();
		DomainSettingsModelHandler.registerType();
		ServersModelHandler.registerType();
		FiltersModelHandler.registerType();
		DomainAssignmentsModelHandler.registerType();
		ImModelHandler.registerType();

		// Subscription
		SubscriptionModelHandler.registerType();
		SubscriptionWidget.registerType();

		// Maintenance
		MaintenanceScreen.registerType();

		// MailflowRules
		EditMailflowRulesEditor.registerType();

		MenuContributor.exportAsfunction("NetBluemindUiAdminconsoleSystemContributor",
				MenuContributor.create(new SystemMenusContributor()));
		ScreenElementContributor.exportAsfunction("NetBluemindUiAdminconsoleSystemScreensContributor",
				ScreenElementContributor.create(new SystemConfScreensContributor()));
	}
}
