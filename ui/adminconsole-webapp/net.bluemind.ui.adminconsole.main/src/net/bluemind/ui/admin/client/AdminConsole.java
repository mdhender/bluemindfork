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
package net.bluemind.ui.admin.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import net.bluemind.authentication.api.IAuthenticationPromise;
import net.bluemind.authentication.api.gwt.endpoint.AuthenticationSockJsEndpoint;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.gwtconsoleapp.base.editor.BasePlugin;
import net.bluemind.gwtconsoleapp.base.lifecycle.GwtAppLifeCycle;
import net.bluemind.gwtconsoleapp.base.lifecycle.ILifeCycle;
import net.bluemind.restbus.api.gwt.RestBusImpl;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.IDomainChangedListener;
import net.bluemind.ui.adminconsole.base.client.AdminCtrl;
import net.bluemind.ui.adminconsole.base.client.RootScreen;
import net.bluemind.ui.adminconsole.base.client.SectionScreen;
import net.bluemind.ui.adminconsole.dataprotect.DataprotectPlugin;
import net.bluemind.ui.adminconsole.directory.DirectoryPlugin;
import net.bluemind.ui.adminconsole.jobs.JobsPlugin;
import net.bluemind.ui.adminconsole.progress.ProgressPlugin;
import net.bluemind.ui.adminconsole.security.SecurityPlugin;
import net.bluemind.ui.adminconsole.system.SystemPlugin;
import net.bluemind.ui.common.client.forms.Ajax;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class AdminConsole implements EntryPoint {

	/**
	 * This is the entry point method.
	 */
	@Override
	public void onModuleLoad() {
		BasePlugin.install();
		RootScreen.registerType();
		SectionScreen.registerType();
		DirectoryPlugin.init();
		DataprotectPlugin.init();
		SystemPlugin.init();
		SecurityPlugin.init();
		ProgressPlugin.init();
		JobsPlugin.init();
		GWT.log("SID from hostPage is " + Ajax.TOKEN.getSessionId());
		GwtAppLifeCycle.registerLifeCycle("net.bluemind.ui.adminconsole.main", new ILifeCycle() {

			@Override
			public void start() {
				RestBusImpl.get().addListener(online -> {
					IAuthenticationPromise auth = new AuthenticationSockJsEndpoint(Ajax.TOKEN.getSessionId())
							.promiseApi();
					auth.getCurrentUser().thenAccept(authUser -> {
						Ajax.setAuthUser(authUser);
						startUpConsole();
					});
				});
			}
		});
	}

	public static void startUpConsole() {
		// create AdminCtrl (floating in the air)
		new AdminCtrl();

		// FIXME dirty, we should wait that application is initialized
		DomainsHolder.get().registerDomainChangedListener(new IDomainChangedListener() {

			@Override
			public void activeDomainChanged(ItemValue<Domain> newActiveDomain) {
				Actions.get().updateLocation();
			}
		});
	}
}
