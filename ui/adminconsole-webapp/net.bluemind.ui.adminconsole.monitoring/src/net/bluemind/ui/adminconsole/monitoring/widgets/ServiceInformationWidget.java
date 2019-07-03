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

import net.bluemind.monitoring.api.MethodInformation;
import net.bluemind.monitoring.api.ServiceInformation;

/**
 * 
 * Widget used to display an information (a list of server information)
 * 
 * @author vincent
 *
 */
public class ServiceInformationWidget extends Composite {

	public ServiceInformation info;
	public ScrollPanel panel;

	public ServiceInformationWidget() {
		this.panel = new ScrollPanel();
		this.initWidget(panel);
	}

	public ServiceInformationWidget(ServiceInformation info) {
		this.info = info;
		this.panel = new ScrollPanel();
		this.initWidget(panel);
	}

	public void init() {
		for (MethodInformation methodInfo : this.info.methodInfoList) {
			this.panel.add(new MethodInformationWidget(methodInfo));
		}
	}

}
