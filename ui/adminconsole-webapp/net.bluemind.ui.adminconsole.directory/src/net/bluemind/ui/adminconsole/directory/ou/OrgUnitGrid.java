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
package net.bluemind.ui.adminconsole.directory.ou;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.ui.admin.client.forms.det.CellHeader;
import net.bluemind.ui.admin.client.forms.det.IBmGrid;
import net.bluemind.ui.admin.client.forms.det.IEditHandler;
import net.bluemind.ui.admin.client.forms.det.RowSelectionEventManager;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;

public class OrgUnitGrid extends DataGrid<OrgUnitPath> implements IBmGrid<OrgUnitPath> {

	private MultiSelectionModel<OrgUnitPath> selectionModel;
	private ProvidesKey<OrgUnitPath> keyProvider;
	private ListDataProvider<OrgUnitPath> ldp;
	private String loc;

	public OrgUnitGrid() {
		loc = LocaleInfo.getCurrentLocale().getLocaleName();
		if (loc.length() > 2) {
			loc = loc.substring(0, 2);
		}

		keyProvider = new ProvidesKey<OrgUnitPath>() {
			@Override
			public Object getKey(OrgUnitPath item) {
				return (item == null) ? null : item.uid;
			}
		};

		selectionModel = new MultiSelectionModel<OrgUnitPath>(keyProvider);
		this.getElement().getStyle().setCursor(Cursor.POINTER);

		IEditHandler<OrgUnitPath> editHandler = new IEditHandler<OrgUnitPath>() {

			@Override
			public SelectAction edit(CellPreviewEvent<OrgUnitPath> cpe) {
				if (cpe.getColumn() == 0) {
					return SelectAction.TOGGLE;
				} else {
					OrgUnitPath path = cpe.getValue();
					Map<String, String> map = new HashMap<>();
					map.put("orgUnitId", path.uid);
					map.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);

					Actions.get().showWithParams2("editOrgUnit", map);
					return SelectAction.IGNORE;
				}
			}
		};

		RowSelectionEventManager<OrgUnitPath> rowSelectionEventManager = RowSelectionEventManager
				.createRowManager(editHandler);

		setSelectionModel(selectionModel, rowSelectionEventManager);

		Column<OrgUnitPath, Boolean> checkColumn = new Column<OrgUnitPath, Boolean>(new CheckboxCell(true, false)) {
			@Override
			public Boolean getValue(OrgUnitPath de) {
				return selectionModel.isSelected(de);
			}
		};
		Header<Boolean> selHead = new CellHeader<OrgUnitPath>(new CheckboxCell(), this, null);
		addColumn(checkColumn, selHead, selHead);
		setColumnWidth(checkColumn, 40, Unit.PX);

		TextColumn<OrgUnitPath> labelColumn = new TextColumn<OrgUnitPath>() {

			@Override
			public String getValue(OrgUnitPath path) {
				String name = null;
				for (OrgUnitPath p = path; p != null; p = p.parent) {
					if (name == null) {
						name = p.name;
					} else {
						name = p.name + "/" + name;
					}
				}
				return name;
			}
		};
		labelColumn.setSortable(true);
		addColumn(labelColumn, "label", "label");
		setColumnWidth(labelColumn, 25.0, Unit.PCT);

		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);

		ldp = new ListDataProvider<>();
		ldp.addDataDisplay(this);

		setPageSize(OrgUnitsBrowser.PAGE_SIZE);
	}

	public Collection<OrgUnitPath> getSelected() {
		return selectionModel.getSelectedSet();
	}

	public HandlerRegistration addSelectionChangeHandler(Handler handler) {
		return selectionModel.addSelectionChangeHandler(handler);
	}

	public void clearSelectionModel() {
		selectionModel.clear();
	}

	@Override
	public void refresh() {
		ldp.refresh();
	}

	@Override
	public void selectAll(boolean b) {
		for (OrgUnitPath d : getValues()) {
			selectionModel.setSelected(d, b);
		}
	}

	@Override
	public List<OrgUnitPath> getValues() {
		return ldp.getList();
	}

	@Override
	public void setValues(List<OrgUnitPath> values) {
		ldp.setList(values);
		ldp.refresh();
	}

}
