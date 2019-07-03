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
package net.bluemind.ui.adminconsole.monitoring.widgets;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ScrollPanel;

import net.bluemind.monitoring.api.PluginInformation;
import net.bluemind.monitoring.api.ServiceInformation;

public class PluginInformationWidget extends Composite {

	public PluginInformation info;
	public ScrollPanel mainPanel;

	public PluginInformationWidget() {
		this.mainPanel = new ScrollPanel();
		this.initWidget(mainPanel);
	}

	public PluginInformationWidget(PluginInformation info) {
		this.info = info;
		this.mainPanel = new ScrollPanel();
		this.initWidget(this.mainPanel);
		this.init();
	}

	private void init() {
		for (ServiceInformation serviceInfo : this.info.serviceInfoList) {
			this.mainPanel.add(new ServiceInformationWidget(serviceInfo));
		}

	}

}
