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
package net.bluemind.ui.adminconsole.system.hosts;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
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

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.Server;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.BulletListCell;
import net.bluemind.ui.adminconsole.base.ui.CellHeader;
import net.bluemind.ui.adminconsole.base.ui.IBmGrid;
import net.bluemind.ui.adminconsole.base.ui.IEditHandler;
import net.bluemind.ui.adminconsole.base.ui.RowSelectionEventManager;
import net.bluemind.ui.adminconsole.system.hosts.l10n.HostConstants;

public class HostsGrid extends DataGrid<ItemValue<Server>>implements IBmGrid<ItemValue<Server>> {

	private MultiSelectionModel<ItemValue<Server>> selectionModel;
	private ListDataProvider<ItemValue<Server>> ldp;
	private ProvidesKey<ItemValue<Server>> keyProvider;

	public HostsGrid() {
		this.keyProvider = createKeyProvider();
		selectionModel = new MultiSelectionModel<ItemValue<Server>>(keyProvider);

		IEditHandler<ItemValue<Server>> editHandler = createEditHandler();

		RowSelectionEventManager<ItemValue<Server>> rowSelectionEventManager = RowSelectionEventManager
				.<ItemValue<Server>> createRowManager(editHandler);
		setSelectionModel(selectionModel, rowSelectionEventManager);

		createColums();

		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);
		setPageSize(Integer.MAX_VALUE);

		this.ldp = new ListDataProvider<ItemValue<Server>>();
		ldp.addDataDisplay(this);
		this.getElement().getStyle().setCursor(Cursor.POINTER);
	}

	private void createColums() {
		Column<ItemValue<Server>, Boolean> checkColumn = new Column<ItemValue<Server>, Boolean>(
				new CheckboxCell(true, false)) {
			@Override
			public Boolean getValue(ItemValue<Server> server) {
				return selectionModel.isSelected(server);
			}
		};
		Header<Boolean> selHead = new CellHeader<ItemValue<Server>>(new CheckboxCell(), this);
		addColumn(checkColumn, selHead, selHead);
		setColumnWidth(checkColumn, 40, Unit.PX);

		TextColumn<ItemValue<Server>> name = new TextColumn<ItemValue<Server>>() {
			@Override
			public String getValue(ItemValue<Server> server) {
				return server.value.name;
			}
		};
		addColumn(name, HostConstants.INST.name(), HostConstants.INST.name());
		setColumnWidth(name, 40, Unit.PCT);

		TextColumn<ItemValue<Server>> ip = new TextColumn<ItemValue<Server>>() {
			@Override
			public String getValue(ItemValue<Server> server) {
				return server.value.ip;
			}
		};
		addColumn(ip, HostConstants.INST.ip(), HostConstants.INST.ip());
		setColumnWidth(ip, 40, Unit.PCT);

		TextColumn<ItemValue<Server>> serverName = new TextColumn<ItemValue<Server>>() {
			@Override
			public String getValue(ItemValue<Server> server) {
				return server.value.fqdn;
			}
		};
		addColumn(serverName, HostConstants.INST.nameColumn(), HostConstants.INST.nameColumn());
		setColumnWidth(serverName, 40, Unit.PCT);

		Column<ItemValue<Server>, Collection<String>> aliases = new Column<ItemValue<Server>, Collection<String>>(
				new BulletListCell()) {
			@Override
			public Collection<String> getValue(ItemValue<Server> server) {
				return server.value.tags;
			}
		};
		addColumn(aliases, "Tags", "Tags");
		setColumnWidth(aliases, 200, Unit.PX);
	}

	private IEditHandler<ItemValue<Server>> createEditHandler() {
		return new IEditHandler<ItemValue<Server>>() {

			@Override
			public SelectAction edit(CellPreviewEvent<ItemValue<Server>> cpe) {
				if (cpe.getColumn() == 0) {
					return SelectAction.TOGGLE;
				} else {
					ItemValue<Server> server = cpe.getValue();
					Map<String, String> params = new HashMap<>();
					params.put(HostKeys.host.name(), server.uid);
					Actions.get().showWithParams2("editHost", params);
					return SelectAction.IGNORE;
				}
			}
		};
	}

	private ProvidesKey<ItemValue<Server>> createKeyProvider() {
		return new ProvidesKey<ItemValue<Server>>() {
			@Override
			public Object getKey(ItemValue<Server> item) {
				if (item == null) {
					return null;
				}
				return item.uid;
			}
		};
	}

	public void setValues(List<ItemValue<Server>> entities) {
		ldp.setList(entities);
		ldp.refresh();
	}

	public List<ItemValue<Server>> getValues() {
		return ldp.getList();
	}

	public void refresh() {
		ldp.refresh();
	}

	public void selectAll(boolean checked) {
		for (ItemValue<Server> s : getValues()) {
			selectionModel.setSelected(s, checked);
		}
	}

	public HandlerRegistration addSelectionChangeHandler(Handler handler) {
		return selectionModel.addSelectionChangeHandler(handler);
	}

	public Collection<ItemValue<Server>> getSelected() {
		return selectionModel.getSelectedSet();
	}

	@Override
	public ProvidesKey<ItemValue<Server>> getKeyProvider() {
		return keyProvider;
	}

	public void clearSelectionModel() {
		selectionModel.clear();
	}

}
