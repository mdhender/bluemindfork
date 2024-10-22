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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.DirEntryQuery.Dir;
import net.bluemind.directory.api.DirEntryQuery.OrderBy;
import net.bluemind.directory.api.DirEntryQuery.StateFilter;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.admin.client.forms.det.IBmGrid;
import net.bluemind.ui.admin.client.forms.det.TippedResource;
import net.bluemind.ui.admin.client.forms.det.TooltipedImageCell;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.directory.IconTips;
import net.bluemind.ui.adminconsole.directory.ou.event.OUCheckBoxEvent;
import net.bluemind.ui.adminconsole.directory.ou.l10n.OrgUnitConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class CommonOrgResourceGrid extends DataGrid<ItemValue<DirEntry>> implements IBmGrid<ItemValue<DirEntry>> {

	protected final OrgUnitListMgmt unitListMngt = OrgUnitListMgmt.get();

	public static final int PAGE_SIZE = 10;

	public interface DCGBundle extends ClientBundle {

		@Source("OrgResourcesGrid.css")
		DCGStyle getStyle();

	}

	public interface DCGStyle extends CssResource {

		public String suspended();

	}

	public static DCGBundle bundle;
	public static DCGStyle style;

	protected SelectionModel<ItemValue<DirEntry>> selectionModel;
	protected ListDataProvider<ItemValue<DirEntry>> ldp;
	protected static final OrgUnitConstants constants = OrgUnitConstants.INST;
	protected TextColumn<ItemValue<DirEntry>> displayNameColumn;
	protected Column<ItemValue<DirEntry>, TippedResource> typeColumn;

	public CommonOrgResourceGrid(String emptyTableLabel, SelectionModel<ItemValue<DirEntry>> selectionModel) {
		bundle = GWT.create(DCGBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();

		this.getElement().getStyle().setCursor(Cursor.POINTER);

		this.selectionModel = new SingleSelectionModel<>(item -> (item == null) ? null : item.uid);
		setSelectionModel(this.selectionModel);

		typeColumn = new Column<ItemValue<DirEntry>, TippedResource>(new TooltipedImageCell()) {

			@Override
			public TippedResource getValue(ItemValue<DirEntry> object) {
				String style = null;
				String tip = null;
				switch (object.value.kind) {
				case GROUP:
					style = "fa-users";
					tip = IconTips.INST.iconTipGroup();
					break;
				case MAILSHARE:
					style = "fa-inbox";
					tip = IconTips.INST.iconTipMailshare();
					break;
				case EXTERNALUSER:
					style = "fa-user-secret";
					tip = IconTips.INST.iconTipExternalUser();
					break;
				case RESOURCE:
					style = "fa-briefcase";
					tip = IconTips.INST.iconTipResource();
					break;
				case CALENDAR:
					style = "fa-calendar";
					break;
				case ADDRESSBOOK:
					style = "fa-book";
					break;
				default:
				case USER:
					style = "fa-user";
					tip = IconTips.INST.iconTipUser();
					break;
				}
				return new TippedResource(style, tip);
			}
		};
		typeColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

		addColumn(typeColumn, constants.resType());
		setColumnWidth(typeColumn, 10, Unit.PX);
		typeColumn.setSortable(true);

		displayNameColumn = new TextColumn<ItemValue<DirEntry>>() {

			@Override
			public String getValue(ItemValue<DirEntry> de) {
				return de.value.displayName;
			}

			@Override
			public String getCellStyleNames(Context context, ItemValue<DirEntry> object) {
				if (object.value.archived) {
					return style.suspended();
				}

				return super.getCellStyleNames(context, object);
			}
		};

		addColumn(displayNameColumn, constants.resName());
		setColumnWidth(displayNameColumn, 60, Unit.PX);
		displayNameColumn.setSortable(true);

		setHeight("250px");

		setEmptyTableWidget(new Label(emptyTableLabel));
		setLoadingIndicator(null);
		setPageSize(PAGE_SIZE);

		ldp = new ListDataProvider<>();
		ldp.addDataDisplay(this);

		// add handler to sorting
		AsyncHandler columnSortHanler = new AsyncHandler(this);
		addColumnSortHandler(columnSortHanler);
	}

	@Override
	public void refresh() {
		ldp.refresh();
	}

	@Override
	public void selectAll(boolean b) {
		// roles cannot be selected
	}

	@Override
	public List<ItemValue<DirEntry>> getValues() {
		return ldp.getList();
	}

	@Override
	public void setValues(List<ItemValue<DirEntry>> values) {
		ldp.setList(values);
		ldp.refresh();
	}

	public TextColumn<ItemValue<DirEntry>> getDisplayNameColumn() {
		return displayNameColumn;
	}

	public void setDisplayNameColumn(TextColumn<ItemValue<DirEntry>> displayNameColumn) {
		this.displayNameColumn = displayNameColumn;
	}

	public Column<ItemValue<DirEntry>, TippedResource> getTypeColumn() {
		return typeColumn;
	}

	public void setTypeColumn(Column<ItemValue<DirEntry>, TippedResource> typeColumn) {
		this.typeColumn = typeColumn;
	}

	protected DirEntryQuery initQuery() {
		DirEntryQuery dq = new DirEntryQuery();
		dq.hiddenFilter = false;
		dq.onlyManagable = true;

		dq.stateFilter = StateFilter.All;
		dq.order = DirEntryQuery.order(OrderBy.displayname, Dir.asc);

		final ColumnSortList sortList = getColumnSortList();
		if (sortList.size() != 0) {
			ColumnSortInfo csi = sortList.get(0);
			if (csi.isAscending()) {
				dq.order.dir = Dir.asc;
			} else {
				dq.order.dir = Dir.desc;
			}

			if (csi.getColumn().equals(getTypeColumn())) {
				dq.order.by = OrderBy.kind;
			} else if (csi.getColumn().equals(getDisplayNameColumn())) {
				dq.order.by = OrderBy.displayname;
			}
		} else {
			sortList.push(getDisplayNameColumn());
		}

		return dq;
	}

	protected void returnEmptyTable(String label) {
		setEmptyTableWidget(new Label(label));
		setValues(Collections.emptyList());
		OrgUnitListMgmt.CHECK_EVENT_BUS.fireEvent(new OUCheckBoxEvent(unitListMngt.hasSelectedItems()));
	}

	protected void doFind(DirEntryQuery dq, DefaultAsyncHandler<ListResult<ItemValue<DirEntry>>> asyncHandler) {
		IDirectoryPromise dir = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(),
				DomainsHolder.get().getSelectedDomain().uid).promiseApi();

		if (dq.orgUnitIds != null && dq.orgUnitIds.isEmpty()) {
			ListResult<ItemValue<DirEntry>> res = new ListResult<>();
			CompletableFuture.completedFuture(res).thenAccept(asyncHandler::success);
			return;
		}

		CompletableFuture<ListResult<ItemValue<DirEntry>>> dirSearch = dir.search(dq);
		dirSearch.thenAccept(dirRet -> {
			ListResult<ItemValue<DirEntry>> res = new ListResult<>();
			res.values = new ArrayList<>();
			res.values.addAll(dirRet.values);
			CompletableFuture.completedFuture(res).thenAccept(asyncHandler::success).exceptionally(t -> {
				asyncHandler.failure(t);
				return null;
			});
		});
	}

}
