/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.directory.ou;

import java.util.stream.Collectors;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.orgunit.OUUtils;
import net.bluemind.ui.adminconsole.directory.ou.event.OUCheckBoxEvent;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitItem;

public class OrgResourceGrid extends CommonOrgResourceGrid {

	public static final String TYPE = "bm.ac.OrgResourceGrid";

	private TextColumn<ItemValue<DirEntry>> emailColumn;
	private TextColumn<ItemValue<DirEntry>> orgUnitColumn;

	public OrgResourceGrid() {
		super(constants.emptyResourceTable(), new NoSelectionModel<>(item -> (item == null) ? null : item.uid));

		emailColumn = new TextColumn<ItemValue<DirEntry>>() {

			@Override
			public String getValue(ItemValue<DirEntry> de) {
				return de.value.email;
			}
		};
		addColumn(emailColumn, constants.resEmail());
		setColumnWidth(emailColumn, 60, Unit.PX);
		emailColumn.setSortable(false);

		orgUnitColumn = new TextColumn<ItemValue<DirEntry>>() {

			@Override
			public String getValue(ItemValue<DirEntry> de) {
				if (de.value.orgUnitPath != null) {
					return OUUtils.toPath(de.value.orgUnitPath);
				} else {
					return "";
				}
			}
		};
		addColumn(orgUnitColumn, constants.resUnit());
		setColumnWidth(orgUnitColumn, 60, Unit.PX);
		orgUnitColumn.setSortable(false);

		setWidth("100%");
	}

	public void loadResourceGridContent(boolean hasSelectedItems, SimplePager pager) {
		if (!hasSelectedItems) {
			returnEmptyTable(constants.emptyResourceTable());
			return;
		}

		AsyncDataProvider<ItemValue<DirEntry>> provider = new AsyncDataProvider<ItemValue<DirEntry>>() {

			@Override
			protected void onRangeChanged(HasData<ItemValue<DirEntry>> display) {

				DirEntryQuery dq = createDirEntryQuery();
				doFind(dq, new DefaultAsyncHandler<ListResult<ItemValue<DirEntry>>>() {
					@Override
					public void success(ListResult<ItemValue<DirEntry>> result) {
						if (result.values.isEmpty()) {
							if (unitListMngt.getSelectedItems().size() > 1) {
								returnEmptyTable(constants.massNotFoundResourceTable(
										String.valueOf(unitListMngt.getSelectedItems().size())));
							} else {
								returnEmptyTable(constants
										.notFoundResourceTable(unitListMngt.getSelectedItems().get(0).getName()));
							}
							return;
						}

						int start = display.getVisibleRange().getStart();
						if (start > result.values.size()) {
							start = 0;
							pager.firstPage();
						}
						updateRowCount(result.values.size(), true);
						updateRowData(start, result.values);

						setValues(result.values);
						OrgUnitListMgmt.CHECK_EVENT_BUS.fireEvent(new OUCheckBoxEvent(unitListMngt.hasSelectedItems()));
					}
				});
			}
		};
		provider.addDataDisplay(this);

	}

	private DirEntryQuery createDirEntryQuery() {
		DirEntryQuery dq = initQuery();
		dq.kindsFilter = java.util.Arrays.asList(Kind.USER, Kind.GROUP, Kind.RESOURCE, Kind.MAILSHARE,
				Kind.EXTERNALUSER, Kind.ADDRESSBOOK, Kind.CALENDAR);
		dq.orgUnitIds = unitListMngt.getSelectedItems().stream().map(OrgUnitItem::getItemId)
				.collect(Collectors.toList());
		return dq;
	}

}
