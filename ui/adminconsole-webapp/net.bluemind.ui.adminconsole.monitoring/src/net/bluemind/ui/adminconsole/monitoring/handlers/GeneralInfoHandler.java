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
import java.util.List;

import com.google.gwt.core.shared.GWT;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainsAsync;
import net.bluemind.domain.api.gwt.endpoint.DomainsGwtEndpoint;
import net.bluemind.group.api.IGroupAsync;
import net.bluemind.group.api.gwt.endpoint.GroupSockJsEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.monitoring.l10n.HandlersConstants;
import net.bluemind.ui.adminconsole.monitoring.screens.GlobalStatusScreen;
import net.bluemind.user.api.IUserAsync;
import net.bluemind.user.api.gwt.endpoint.UserSockJsEndpoint;

/**
 * This handler fetches information concerning the CPU and the RAM
 * 
 * @author vincent
 *
 */
public class GeneralInfoHandler extends DefaultAsyncHandler<List<ItemValue<Domain>>> {

	public HandlersConstants text;

	public Integer userCount;
	public Integer groupCount;

	public List<ItemValue<Domain>> allDomains;
	public List<IGroupAsync> groupEndpoints;
	public List<IUserAsync> userEndpoints;

	public GlobalStatusScreen screen;

	public IDomainsAsync domainsEndpoint;

	public GeneralInfoHandler(GlobalStatusScreen screen) {
		this.screen = screen;

		this.text = GWT.create(HandlersConstants.class);

		this.domainsEndpoint = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());

		this.groupEndpoints = new ArrayList<>();
		this.userEndpoints = new ArrayList<>();

		this.userCount = 0;
		this.groupCount = 0;

	}

	@Override
	public void success(List<ItemValue<Domain>> value) {
		this.allDomains = value;

		fillEndpoints();
		countUsers();
		countGroups();
	}

	private void countUsers() {
		for (IUserAsync endpoint : this.userEndpoints) {
			endpoint.allUids(new DefaultAsyncHandler<List<String>>() {
				@Override
				public void success(List<String> value) {
					userCount += value.size();
					screen.userCount.getElement().setInnerHTML(String.valueOf(userCount) + " " + text.userCount());
				}
			});
		}
	}

	private void countGroups() {
		for (IGroupAsync endpoint : this.groupEndpoints) {
			endpoint.allUids(new DefaultAsyncHandler<List<String>>() {
				@Override
				public void success(List<String> value) {
					groupCount += value.size();
					screen.groupCount.getElement().setInnerHTML(String.valueOf(groupCount) + " " + text.groupCount());
				}
			});
		}
	}

	private void fillEndpoints() {
		for (ItemValue<Domain> domain : this.allDomains) {
			this.groupEndpoints.add(new GroupSockJsEndpoint(Ajax.TOKEN.getSessionId(), domain.uid));
			this.userEndpoints.add(new UserSockJsEndpoint(Ajax.TOKEN.getSessionId(), domain.uid));
		}
	}

}
