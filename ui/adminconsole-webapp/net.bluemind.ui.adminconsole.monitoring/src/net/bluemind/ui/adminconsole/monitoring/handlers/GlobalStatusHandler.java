/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.monitoring.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.core.shared.GWT;

import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.monitoring.api.MethodInformation;
import net.bluemind.monitoring.api.PluginInformation;
import net.bluemind.monitoring.api.PluginsList;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.ServiceInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.ui.adminconsole.monitoring.l10n.HandlersConstants;
import net.bluemind.ui.adminconsole.monitoring.models.ServerInformationMessageEntry;
import net.bluemind.ui.adminconsole.monitoring.screens.GlobalStatusScreen;

/**
 * 
 * The Global Status handler fetches and populates the message list box widget
 * with all the messages that were generated from the API
 * 
 * @author vincent
 *
 */
public class GlobalStatusHandler extends DefaultAsyncHandler<PluginsList> {

	public HandlersConstants text;
	public Status status;
	public GlobalStatusScreen screen;

	public GlobalStatusHandler(GlobalStatusScreen screen) {
		this.text = GWT.create(HandlersConstants.class);
		this.screen = screen;
	}

	@Override
	public void success(PluginsList allPluginsListInfo) {
		List<ServerInformationMessageEntry> data = new ArrayList<ServerInformationMessageEntry>();

		for (PluginInformation pluginInfo : allPluginsListInfo.pluginsList) {
			for (ServiceInformation serviceInfo : pluginInfo.serviceInfoList) {
				for (MethodInformation info : serviceInfo.methodInfoList) {
					if (info.serverInfoList != null) {
						for (ServerInformation srvInfo : info.serverInfoList) {
							if (srvInfo.messages != null) {
								for (int i = 0; i < srvInfo.messages.size(); i++) {
									data.add(new ServerInformationMessageEntry(srvInfo, i));
								}
							} else {
								data.add(new ServerInformationMessageEntry(srvInfo, -1));
							}
						}
					}
				}
			}
		}

		this.screen.listBox.fullUpdate(data);
		this.screen.listBox.filterView(this.screen.filters);

	}

	@Override
	public void failure(Throwable e) {
		// refresh view to remove "throbble"
		this.screen.listBox.fullUpdate(Collections.emptyList());
		this.screen.listBox.filterView(this.screen.filters);
		super.failure(e);
	}

}
