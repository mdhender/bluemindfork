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
package net.bluemind.ui.adminconsole.directory.group;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gwt.core.client.JsArray;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.group.api.gwt.js.JsMember;
import net.bluemind.ui.admin.client.forms.det.SimpleBaseDirEntryFinder;
import net.bluemind.ui.common.client.forms.Ajax;

public class MembersFinder implements SimpleBaseDirEntryFinder {

	private List<JsMember> members = new LinkedList<>();
	private DirectoryGwtEndpoint directory;
	private List<String> filterOut;

	public MembersFinder() {
		directory = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), Ajax.TOKEN.getContainerUid());
	}

	@Override
	public void find(DirEntryQuery tQuery, final AsyncHandler<ListResult<DirEntry>> cb) {

		if (members.isEmpty()) {
			cb.success(ListResult.create(java.util.Collections.emptyList()));
			return;
		}

		int to = Math.min(tQuery.from + tQuery.size, members.size());
		// Don't use query pagination as we filter expected members
		tQuery.from = 0;
		tQuery.size = 0;
		tQuery.entryUidFilter = members.subList(tQuery.from, to).stream() //
				.map(m -> m.getUid()) //
				.collect(Collectors.toList());

		directory.search(tQuery, new AsyncHandler<ListResult<ItemValue<DirEntry>>>() {

			@Override
			public void success(ListResult<ItemValue<DirEntry>> value) {
				ListResult<DirEntry> ret = new ListResult<>();
				ret.total = members.size();
				if (filterOut != null && !filterOut.isEmpty()) {
					ret.values = value.values.stream().filter(v -> !filterOut.contains(v.value.entryUid))
							.map(d -> d.value).collect(Collectors.toList());
				} else {
					ret.values = value.values.stream().map(d -> d.value).collect(Collectors.toList());
				}

				cb.success(ret);
			}

			@Override
			public void failure(Throwable e) {
				cb.failure(e);
			}
		});

	}

	public void setMembers(JsArray<JsMember> m) {
		List<JsMember> list = new ArrayList<>(m.length());
		for (int i = 0; i < m.length(); i++) {
			list.add(m.get(i));
		}
		members = list;
	}

	public void addMember(JsMember m) {
		members.add(m);
	}

	public void removeMember(JsMember m) {
		Optional<JsMember> toRemove = members.stream().filter(member -> member.getUid().equals(m.getUid())).findFirst();
		toRemove.ifPresent(member -> members.remove(member));
	}

	@Override
	public void setDomain(String domain) {
		directory = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domain);
	}

	public List<String> getMembers() {
		return members.stream().map(m -> m.getUid()).collect(Collectors.toList());
	}

	@Override
	public void setFilterOut(List<String> filterOut) {
		this.filterOut = filterOut;
	}
}
