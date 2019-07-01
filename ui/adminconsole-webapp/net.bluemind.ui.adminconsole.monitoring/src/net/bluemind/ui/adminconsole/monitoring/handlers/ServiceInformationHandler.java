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

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.monitoring.api.MethodInformation;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.ServiceInformation;
import net.bluemind.ui.adminconsole.monitoring.l10n.HandlersConstants;
import net.bluemind.ui.adminconsole.monitoring.widgets.ServerInformationWidget;
import net.bluemind.ui.adminconsole.monitoring.widgets.ServiceInformationWidget;

/**
 * An information handler is created every time a single information is viewed
 * by the administrator. It is created after a successful call to the monitoring
 * API and generates an information widget usually used in the information
 * screen.
 * 
 * 
 * @author vincent
 *
 */
public class ServiceInformationHandler extends DefaultAsyncHandler<ServiceInformation> {

	public ServiceInformation info;
	/**
	 * The text constants
	 */
	public HandlersConstants text;
	/**
	 * The widget to be filled with the information
	 */
	public ServiceInformationWidget widget;

	public ServiceInformationHandler(ServiceInformationWidget widget) {
		super();
		this.text = GWT.create(HandlersConstants.class);
		this.widget = widget;
	}

	@Override
	public void success(ServiceInformation info) {
		this.info = info;

		Panel p = new FlowPanel();
		for (MethodInformation i : info.methodInfoList) {

			for (ServerInformation srvInfo : i.serverInfoList) {
				ServerInformationWidget srvWidget = new ServerInformationWidget(srvInfo);

				p.add(srvWidget);
			}
		}
		this.widget.panel.add(p);
	}

	@Override
	public void failure(Throwable e) {
		super.failure(e);
		this.widget.panel.add(new Label(text.resourceNotFound()));
	}

}
