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
import java.util.List;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectoryAsync;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.domain.api.Domain;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder;

public class DirEntryFinder implements IEntityFinder<DirEntry, DirEntryQuery> {

	protected IDirectoryAsync directory;
	private List<Kind> kinds;
	private String domainUid;
	private int limit = 10;

	public DirEntryFinder(List<DirEntry.Kind> kinds) {
		this(kinds, 10);
	}

	public DirEntryFinder(List<DirEntry.Kind> kinds, int limit) {
		this.kinds = kinds;
		this.limit = limit;
		setDomain(Ajax.TOKEN.getContainerUid());
	}

	public DirEntryFinder() {
		setDomain(Ajax.TOKEN.getContainerUid());
	}

	@Override
	public String toString(DirEntry result) {
		if (result.email != null && !result.email.isEmpty()) {
			return result.displayName + " (" + result.email + ")";
		} else {
			return result.displayName;
		}
	}

	public void setDomain(ItemValue<Domain> domain) {
		setDomain(domain.uid);
	}

	@Override
	public void find(DirEntryQuery tQuery, final AsyncHandler<ListResult<DirEntry>> cb) {
		tQuery.hiddenFilter = false;
		// dedataprovider only sets domain when token is not global
		directory.search(tQuery, new AsyncHandler<ListResult<ItemValue<DirEntry>>>() {

			@Override
			public void success(ListResult<ItemValue<DirEntry>> value) {
				ListResult<DirEntry> ret = new ListResult<>();
				ret.total = value.total;
				ret.values = new ArrayList<>(value.values.size());
				for (ItemValue<DirEntry> entry : value.values) {
					ret.values.add(entry.value);
				}

				cb.success(ret);
			}

			@Override
			public void failure(Throwable e) {
				cb.failure(e);
			}
		});
	}

	@Override
	public DirEntryQuery queryFromString(String queryString) {

		DirEntryQuery dq = new DirEntryQuery();
		dq.kindsFilter = getTypeFilder();
		if (net.bluemind.gwtconsoleapp.base.editor.Ajax.TOKEN.isDomainAdmin(domainUid)) {
			dq.hiddenFilter = false;
		}

		dq.size = limit;
		dq.nameOrEmailFilter = queryString;
		return dq;
	}

	protected List<Kind> getTypeFilder() {
		return kinds;
	}

	public String getType(DirEntry de) {
		return de.kind.name();
	}

	@Override
	public void reload(Collection<DirEntry> ids,
			net.bluemind.ui.common.client.forms.autocomplete.IEntityFinder.ReloadCb<DirEntry> cb) {

	}

	@Override
	public void setDomain(String domain) {
		this.domainUid = domain;
		directory = directoryEndpoint(domain);
	}

	protected IDirectoryAsync directoryEndpoint(String domain) {
		return new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domain);
	}

}
