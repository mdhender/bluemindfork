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
package net.bluemind.calendar.service.internal;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.calendar.api.IFreebusyMgmt;
import net.bluemind.calendar.persistence.FreebusyStore;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;

public class FreebusyMgmtService implements IFreebusyMgmt {

	private final FreebusyStore store;
	private final Container container;
	private final RBACManager rbacManager;
	private final IContainers containerService;

	public FreebusyMgmtService(BmContext context, Container container) {
		this.store = new FreebusyStore(context.getDataSource(), container);
		this.rbacManager = RBACManager.forContext(context).forContainer(container);
		this.container = container;
		this.containerService = context.su().provider().instance(IContainers.class);
	}

	@Override
	public void add(String calendar) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		try {
			if (verifyCalendar(calendar)) {
				store.add(calendar);
			}
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void remove(String calendar) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		try {
			store.remove(calendar);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<String> get() throws ServerFault {
		rbacManager.check(Verb.Read.name());

		try {
			return store.get();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void set(List<String> calendars) throws ServerFault {
		rbacManager.check(Verb.Write.name());

		try {
			store.set(calendars.stream().filter(this::verifyCalendar).collect(Collectors.toList()));
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	private boolean verifyCalendar(String calendar) {
		String owner = container.owner;
		ContainerDescriptor calContainer = containerService.getIfPresent(calendar);
		return calContainer != null && calContainer.owner.equals(owner);
	}

}
