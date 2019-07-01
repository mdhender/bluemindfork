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

package net.bluemind.ui.admin.client.forms.det;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MultiSelectionModel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.DirEntryQuery.Dir;
import net.bluemind.directory.api.DirEntryQuery.OrderBy;

public class DEDataProvider extends AsyncDataProvider<DirEntry> {

	private static final int PAGE_SIZE = 25;

	private DETable cellTable;
	private MultiSelectionModel<DirEntry> selectionModel;
	private Collection<DirEntry.Kind> entriesKind;
	private ValueBoxEditor<String> filter;
	private SimpleBaseDirEntryFinder finder;
	private HasData<DirEntry> disp;
	private Set<DirEntry> exclude;
	private Set<DirEntry> include;

	public DEDataProvider(DETable table, MultiSelectionModel<DirEntry> selectionModel,
			Collection<DirEntry.Kind> entriesKind, ValueBoxEditor<String> filter, SimpleBaseDirEntryFinder finder) {
		cellTable = table;
		this.selectionModel = selectionModel;
		this.entriesKind = entriesKind;
		this.filter = filter;
		this.finder = finder;
		exclude = null;
		include = null;
	}

	@Override
	protected void onRangeChanged(HasData<DirEntry> display) {
		disp = display;
		find();
	}

	private DirEntryQuery.Order getSort() {
		// FIXME sort by col
		DirEntryQuery.order(OrderBy.displayname, Dir.asc);
		final ColumnSortList sortList = cellTable.getColumnSortList();
		GWT.log("size SORT " + sortList.size());
		return DirEntryQuery.order(OrderBy.displayname, Dir.asc);
	}

	public void exclude(Set<DirEntry> exclude) {
		this.exclude = exclude;
		if (include != null) {
			for (BaseDirEntry de : exclude) {
				if (include.contains(de)) {
					include.remove(de);
				}
			}
		}
		find();
	}

	public void include(Set<DirEntry> include) {
		this.include = include;
		if (exclude != null) {
			for (BaseDirEntry de : include) {
				if (exclude.contains(de)) {
					exclude.remove(de);
				}
			}
		}
		find();
	}

	private void find() {

		DirEntryQuery dq = new DirEntryQuery();

		// FIXME order?
		// DirEntryQuery.Order dqSort = getSort();
		// dq.order = dqSort;

		final int start = disp.getVisibleRange().getStart();
		int page = start / PAGE_SIZE;
		GWT.log("START " + start + " PAGE: " + page);

		dq.size = PAGE_SIZE;
		dq.from = page * PAGE_SIZE;
		
		dq.hiddenFilter = false;
		
		dq.kindsFilter = new ArrayList<>(entriesKind);

		String filterField = filter.getValue();
		if (filterField != "") {
			dq.nameFilter = filterField;
		}

		finder.find(dq, new AsyncHandler<ListResult<DirEntry>>() {

			@Override
			public void success(ListResult<DirEntry> result) {
				GWT.log("find OK, DirectoryEntities size = " + result.total);
				selectionModel.clear();
				updateRowData(start, result.values);
				updateRowCount((int) result.total, true);
			}

			@Override
			public void failure(Throwable e) {
				// TODO Auto-generated method stub
				// FIMXE handle error
			}
		});
	}

}
