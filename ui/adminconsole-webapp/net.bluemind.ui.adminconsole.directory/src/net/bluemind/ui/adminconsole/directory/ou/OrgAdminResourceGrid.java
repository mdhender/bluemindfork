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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.SingleSelectionModel;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.directory.ou.event.OUCheckBoxEvent;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitItem;
import net.bluemind.ui.common.client.forms.Ajax;

public class OrgAdminResourceGrid extends CommonOrgResourceGrid {

	public static final String TYPE = "bm.ac.OrgAdminResourceGrid";

	public OrgAdminResourceGrid() {
		super(constants.emptyRoleTable(), new SingleSelectionModel<>(item -> (item == null) ? null : item.uid));
	}

	public void reload(SimplePager pager) {
		loadRoleGridContent(pager);
	}

	private void loadRoleGridContent(SimplePager pager) {
		boolean tooManyItems = unitListMngt.getSelectedEnabledItems().size() > 1;
		if (tooManyItems) {
			returnEmptyTable(constants.massRoleOuSelection());
			return;
		}
		if (!unitListMngt.hasSelectedItems()) {
			returnEmptyTable(constants.emptyRoleTable());
			return;
		}

		AsyncDataProvider<ItemValue<DirEntry>> provider = new AsyncDataProvider<ItemValue<DirEntry>>() {

			IOrgUnitsPromise dir = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(),
					DomainsHolder.get().getSelectedDomain().uid).promiseApi();

			@Override
			protected void onRangeChanged(HasData<ItemValue<DirEntry>> display) {
				OrgUnitItem focusedItem = unitListMngt.focusedItem;
				CompletableFuture<Set<String>> administratorsCf = dir.getAdministrators(focusedItem.getUid());

				administratorsCf.thenAccept(admin -> {
					if (admin.isEmpty()) {
						returnEmptyTable(constants.emptyRoleAdminTable(focusedItem.getName()));
						return;
					}

					DirEntryQuery dq = createDirEntryQuery(new ArrayList<>(admin));
					doFind(dq, new DefaultAsyncHandler<ListResult<ItemValue<DirEntry>>>() {
						@Override
						public void success(ListResult<ItemValue<DirEntry>> result) {
							int start = display.getVisibleRange().getStart();
							if (start > result.values.size()) {
								start = 0;
								pager.firstPage();
							}
							updateRowCount(result.values.size(), true);
							updateRowData(start, result.values);

							setValues(result.values);

							OrgUnitListMgmt.CHECK_EVENT_BUS
									.fireEvent(new OUCheckBoxEvent(unitListMngt.hasSelectedItems()));
						}
					});

				});
			}
		};
		provider.addDataDisplay(this);
	}

	private DirEntryQuery createDirEntryQuery(List<String> adminUids) {
		DirEntryQuery dq = initQuery();
		dq.entries = adminUids;
		dq.kindsFilter = java.util.Arrays.asList(Kind.USER, Kind.GROUP);
		return dq;
	}

}
