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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.SingleSelectionModel;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.orgunit.OUUtils;
import net.bluemind.ui.adminconsole.directory.ou.event.OUCheckBoxEvent;
import net.bluemind.ui.adminconsole.directory.ou.event.OUDirEntryEditEvent;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitItem;

public class OrgResourceGrid extends CommonOrgResourceGrid {

	public static final String TYPE = "bm.ac.OrgResourceGrid";

	private TextColumn<ItemValue<DirEntry>> emailColumn;
	private TextColumn<ItemValue<DirEntry>> orgUnitColumn;

	public OrgResourceGrid() {
		super(constants.emptyResourceTable(), new SingleSelectionModel<>(item -> (item == null) ? null : item.uid));

		addCellPreviewHandler(event -> {
			if (BrowserEvents.CLICK.equalsIgnoreCase(event.getNativeEvent().getType())) {
				OrgUnitListMgmt.DIRECTORY_EDIT_BUS.fireEvent(new OUDirEntryEditEvent(event.getValue()));
			}
		});

		emailColumn = new TextColumn<ItemValue<DirEntry>>() {

			@Override
			public String getValue(ItemValue<DirEntry> de) {
				return de.value.email;
			}

			@Override
			public String getCellStyleNames(Context context, ItemValue<DirEntry> object) {
				if (object.value.archived) {
					return style.suspended();
				}

				return super.getCellStyleNames(context, object);
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

			@Override
			public String getCellStyleNames(Context context, ItemValue<DirEntry> object) {
				if (object.value.archived) {
					return style.suspended();
				}

				return super.getCellStyleNames(context, object);
			}
		};
		addColumn(orgUnitColumn, constants.resUnit());
		setColumnWidth(orgUnitColumn, 60, Unit.PX);
		orgUnitColumn.setSortable(false);

		setWidth("100%");
	}

	public void loadResourceGridContent(boolean hasSelectedItems, SimplePager pager) {

		AsyncDataProvider<ItemValue<DirEntry>> provider = new AsyncDataProvider<ItemValue<DirEntry>>() {
			@Override
			protected void onRangeChanged(HasData<ItemValue<DirEntry>> display) {
				if (!hasSelectedItems) {
					returnEmptyTable(constants.emptyResourceTable());
				} else {
					DirEntryQuery dq = createDirEntryQuery();
					doFind(dq, new DefaultAsyncHandler<ListResult<ItemValue<DirEntry>>>() {
						@Override
						public void success(ListResult<ItemValue<DirEntry>> result) {
							if (result != null && !result.values.isEmpty()) {
								int start = display.getVisibleRange().getStart();
								if (start > result.values.size()) {
									start = 0;
									pager.firstPage();
								}
								updateRowCount(result.values.size(), true);
								updateRowData(start, result.values);

								setValues(result.values);
								OrgUnitListMgmt.CHECK_EVENT_BUS.fireEvent(new OUCheckBoxEvent(true));
							}
						}
					});
				}
			}
		};
		provider.addDataDisplay(this);

	}

	public void openDirEntryInNewTab(DirEntry de) {
		String url = "index.html#";
		Map<String, String> params = new HashMap<>();

		switch (de.kind) {
		case MAILSHARE: {
			params.put("entryUid", de.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			url += "editMailshare?";
		}
			break;
		case EXTERNALUSER: {
			params.put("entryUid", de.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			url += "editExternalUser?";
		}
			break;
		case GROUP: {
			params.put("entryUid", de.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			url += "editGroup?";
		}
			break;
		case RESOURCE: {
			params.put("entryUid", de.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			url += "editResource?";
		}
			break;
		case USER: {
			params.put("entryUid", de.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			url += "editUser?";
			break;
		}
		case ADDRESSBOOK: {
			params.put("entryUid", de.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			url += "editBook?";
			break;
		}

		case CALENDAR: {
			params.put("entryUid", de.entryUid);
			params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
			url += "editCalendar?";
			break;
		}
		default:
			break;
		}

		if (!params.isEmpty()) {
			for (Entry<String, String> entry : params.entrySet()) {
				url += entry.getKey() + "=" + entry.getValue() + "&";
			}
			Window.open(url, "_blank", "");
		}
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
