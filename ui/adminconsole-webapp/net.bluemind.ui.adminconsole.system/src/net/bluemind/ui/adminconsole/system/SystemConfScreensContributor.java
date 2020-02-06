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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.ui.adminconsole.system.domains.DomainsScreenContributions;
import net.bluemind.ui.adminconsole.system.hosts.HostsScreenContributor;
import net.bluemind.ui.adminconsole.system.hosts.create.CreateHostScreenContributor;
import net.bluemind.ui.adminconsole.system.hosts.edit.EditHostScreens;
import net.bluemind.ui.adminconsole.system.maintenance.MaintenanceScreen;
import net.bluemind.ui.adminconsole.system.maintenance.reindex.ReindexScreen;
import net.bluemind.ui.adminconsole.system.maintenance.update.UpdateScreen;
import net.bluemind.ui.adminconsole.system.subscription.SubscriptionWidget;
import net.bluemind.ui.adminconsole.system.systemconf.SystemConfScreenContributor;

public class SystemConfScreensContributor implements ScreenElementContributorUnwrapper {

	@Override
	public JsArray<ScreenElementContribution> contribution() {
		JsArray<ScreenElementContribution> systemConfContribs = SystemConfScreenContributor.contribution();

		JsArray<ScreenElementContribution> hostsContribs = HostsScreenContributor.contribution();

		JsArray<ScreenElementContribution> edithostContribs = EditHostScreens.contribution();

		JsArray<ScreenElementContribution> createhostContribs = CreateHostScreenContributor.contribution();

		JsArray<ScreenElementContribution> domainsContribs = DomainsScreenContributions.contribution();

		JsArray<ScreenElementContribution> join = join(systemConfContribs, hostsContribs, edithostContribs,
				createhostContribs, domainsContribs);

		join.push(ScreenElementContribution.create(null, null, SubscriptionWidget.screenModel()));
		join.push(ScreenElementContribution.create(null, null, MaintenanceScreen.screenModel()));
		join.push(ScreenElementContribution.create(null, null, UpdateScreen.screenModel()));
		join.push(ScreenElementContribution.create(null, null, ReindexScreen.screenModel()));

		for (int i = 0; i < join.length(); i++) {
			ScreenElementContribution c = join.get(i);
			GWT.log("System contribution: " + c.toString());
		}
		return join;
	}

	@SafeVarargs
	private final <T extends JavaScriptObject> JsArray<T> join(JsArray<T>... arrays) {
		JsArray<T> ret = JsArray.createArray().cast();
		for (int i = 0; i < arrays.length; i++) {
			JsArray<T> a = arrays[i];
			for (int j = 0; j < a.length(); j++) {
				ret.push(a.get(j));
			}
		}
		return ret;
	}
}
