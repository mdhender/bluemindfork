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
package net.bluemind.ui.common.client.forms.finder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServerAsync;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.gwt.endpoint.ServerGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder;

public class ServerFinder implements IEntityFinder<ItemValue<Server>, Void> {

	private IServerAsync servers;
	private String domainUid;
	private String tagFilter;

	public ServerFinder(String tagFilter) {
		this.tagFilter = tagFilter;
		servers = new ServerGwtEndpoint(Ajax.TOKEN.getSessionId(), "default");
	}

	@Override
	public String toString(ItemValue<Server> result) {
		return result.displayName;
	}

	public void setDomain(String domainUid) {
		this.domainUid = domainUid;
	}

	@Override
	public void find(Void tQuery, final AsyncHandler<ListResult<ItemValue<Server>>> cb) {
		// dedataprovider only sets domain when token is not global
		if (domainUid == null) {
			cb.failure(null);
			return;
		}
		servers.allComplete(new AsyncHandler<List<ItemValue<Server>>>() {

			@Override
			public void success(final List<ItemValue<Server>> listOfServers) {

				servers.getAssignments(domainUid, new AsyncHandler<List<Assignment>>() {

					@Override
					public void success(List<Assignment> value) {

						cb.success(process(value, listOfServers));
						// TODO Auto-generated method stub

					}

					@Override
					public void failure(Throwable e) {
						cb.failure(e);
					}
				});
			}

			@Override
			public void failure(Throwable e) {
				cb.failure(e);
			}
		});
	}

	public void find(String domainUid, ListBox box, HTMLPanel parent) {
		setDomain(domainUid);
		if (domainUid.equals("global.virt")) {
			box.clear();
			parent.setVisible(false);
			return;
		}
		this.find(null, new AsyncHandler<ListResult<ItemValue<Server>>>() {

			@Override
			public void success(ListResult<ItemValue<Server>> value) {
				box.clear();
				for (ItemValue<Server> s : value.values) {
					box.addItem(s.value.name + " (" + s.value.address() + ")", s.uid);
				}
				if (box.getItemCount() > 0) {
					box.setSelectedIndex(0);
				}
				if (box.getItemCount() < 2) {
					parent.setVisible(false);
				}
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}

		});
	}

	private ListResult<ItemValue<Server>> process(List<Assignment> value, List<ItemValue<Server>> listOfServers) {

		List<ItemValue<Server>> ret = new ArrayList<ItemValue<Server>>(listOfServers.size());
		Map<String, ItemValue<Server>> servers = new HashMap<>();
		for (ItemValue<Server> server : listOfServers) {
			servers.put(server.uid, server);
		}
		for (Assignment ass : value) {
			if (tagFilter != null) {
				if (tagFilter.equals(ass.tag)) {
					ret.add(servers.get(ass.serverUid));
				}
			} else {
				ret.add(servers.get(ass.serverUid));
			}
		}
		return ListResult.create(ret);
	}

	@Override
	public Void queryFromString(String queryString) {
		return null;
	}

	@Override
	public String getType(ItemValue<Server> result) {
		return "server"; // FIXME ???
	}

	@Override
	public void reload(Collection<ItemValue<Server>> ids,
			net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder.ReloadCb<ItemValue<Server>> cb) {
		// TODO Auto-generated method stub

	}

	public void setTagFilter(String tagFilter) {
		this.tagFilter = tagFilter;
	}

}
