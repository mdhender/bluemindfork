/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectoryAsync;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.ui.admin.client.forms.det.SimpleBaseDirEntryFinder;
import net.bluemind.ui.common.client.forms.Ajax;

public class UserOrExternalUserSimpleFinder implements SimpleBaseDirEntryFinder {

	protected IDirectoryAsync directory;
	private Set<String> filterOut;

	@Override
	public void find(DirEntryQuery tQuery, AsyncHandler<ListResult<DirEntry>> cb) {
		int from = tQuery.from;
		int size = tQuery.size;
		tQuery.from = 0;
		if (size != -1) {
			tQuery.size = from + size + 1000;
		}
		search(tQuery, cb, from, size);

	}

	private void search(DirEntryQuery tQuery, AsyncHandler<ListResult<DirEntry>> cb, int from, int size) {

		directory.search(tQuery, new AsyncHandler<ListResult<ItemValue<DirEntry>>>() {

			@Override
			public void success(ListResult<ItemValue<DirEntry>> value) {
				ListResult<DirEntry> ret = new ListResult<>();

				// filter out assigned elements
				if (filterOut != null && !filterOut.isEmpty()) {
					ret.values = value.values.stream().filter(v -> !filterOut.contains(v.value.entryUid))
							.map(d -> d.value).collect(Collectors.toList());
				} else {
					ret.values = value.values.stream().map(d -> d.value).collect(Collectors.toList());
				}

				long totalSize = size;
				if (totalSize == -1) {
					totalSize = ret.total;
				}

				// detect total size
				long maxPossibleSize = value.total;
				if (filterOut != null && (tQuery.nameFilter == null || tQuery.nameFilter.isEmpty())) {
					// non-filtered search. size is result.total - assigned elements
					maxPossibleSize = maxPossibleSize - filterOut.size() + 1;
				} else {
					// filtered search
					maxPossibleSize = ret.values.size();
				}
				// pagination
				ret.values = ret.values.subList(from,
						Math.min(ret.values.size(), Math.min(from + (int) totalSize, ret.values.size())));
				if (ret.values.size() > size) {
					ret.values = ret.values.subList(0, size);
				}

				// non-filtered search. continue to search of requested size has not been
				// reached
				if (ret.values.size() < maxPossibleSize && ret.values.size() < size && tQuery.size < value.total) {
					tQuery.size += 1000;
					search(tQuery, cb, from, size);
				} else {
					ret.total = maxPossibleSize;
					cb.success(ret);
				}
			}

			@Override
			public void failure(Throwable e) {
				cb.failure(e);
			}
		});
	}

	@Override
	public void setDomain(String domain) {
		directory = directoryEndpoint(domain);
	}

	protected IDirectoryAsync directoryEndpoint(String domain) {
		return new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domain);
	}

	@Override
	public void setFilterOut(List<String> filterOut) {
		this.filterOut = new HashSet<>(filterOut);
	}

}
