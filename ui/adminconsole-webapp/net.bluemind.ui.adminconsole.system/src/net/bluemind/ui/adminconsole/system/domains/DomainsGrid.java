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
package net.bluemind.ui.adminconsole.system.domains;

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
import net.bluemind.domain.api.Domain;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.BulletListCell;
import net.bluemind.ui.adminconsole.base.ui.CellHeader;
import net.bluemind.ui.adminconsole.base.ui.IBmGrid;
import net.bluemind.ui.adminconsole.base.ui.IEditHandler;
import net.bluemind.ui.adminconsole.base.ui.RowSelectionEventManager;
import net.bluemind.ui.adminconsole.system.domains.l10n.DomainConstants;

public class DomainsGrid extends DataGrid<ItemValue<Domain>> implements IBmGrid<ItemValue<Domain>> {

	private MultiSelectionModel<ItemValue<Domain>> selectionModel;
	private ListDataProvider<ItemValue<Domain>> ldp;
	private ProvidesKey<ItemValue<Domain>> keyProvider;

	public DomainsGrid() {
		this.keyProvider = createKeyProvider();
		selectionModel = new MultiSelectionModel<>(keyProvider);

		IEditHandler<ItemValue<Domain>> editHandler = createEditHandler();

		RowSelectionEventManager<ItemValue<Domain>> rowSelectionEventManager = RowSelectionEventManager
				.<ItemValue<Domain>>createRowManager(editHandler);
		setSelectionModel(selectionModel, rowSelectionEventManager);

		createColums();

		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);
		setPageSize(Integer.MAX_VALUE);

		this.ldp = new ListDataProvider<ItemValue<Domain>>();
		ldp.addDataDisplay(this);
		this.getElement().getStyle().setCursor(Cursor.POINTER);
	}

	private void createColums() {
		Column<ItemValue<Domain>, Boolean> checkColumn = new Column<ItemValue<Domain>, Boolean>(
				new CheckboxCell(true, false)) {
			@Override
			public Boolean getValue(ItemValue<Domain> Domain) {
				return selectionModel.isSelected(Domain);
			}
		};
		Header<Boolean> selHead = new CellHeader<ItemValue<Domain>>(new CheckboxCell(), this);
		addColumn(checkColumn, selHead, selHead);
		setColumnWidth(checkColumn, 40, Unit.PX);

		TextColumn<ItemValue<Domain>> defaultAlias = new TextColumn<ItemValue<Domain>>() {
			@Override
			public String getValue(ItemValue<Domain> domain) {
				return domain.value.defaultAlias;
			}
		};
		addColumn(defaultAlias, DomainConstants.INST.defaultAlias(), DomainConstants.INST.defaultAlias());
		setColumnWidth(defaultAlias, 40, Unit.PCT);

		TextColumn<ItemValue<Domain>> description = new TextColumn<ItemValue<Domain>>() {
			@Override
			public String getValue(ItemValue<Domain> domain) {
				return domain.value.description;
			}
		};
		addColumn(description, DomainConstants.INST.description(), DomainConstants.INST.description());
		setColumnWidth(description, 40, Unit.PCT);

		TextColumn<ItemValue<Domain>> uid = new TextColumn<ItemValue<Domain>>() {
			@Override
			public String getValue(ItemValue<Domain> domain) {
				return domain.uid;
			}
		};
		addColumn(uid, DomainConstants.INST.name(), DomainConstants.INST.name());
		setColumnWidth(uid, 20, Unit.PCT);

		Column<ItemValue<Domain>, Collection<String>> aliases = new Column<ItemValue<Domain>, Collection<String>>(
				new BulletListCell()) {
			@Override
			public Collection<String> getValue(ItemValue<Domain> domain) {
				return domain.value.aliases;
			}
		};
		addColumn(aliases, DomainConstants.INST.aliases(), DomainConstants.INST.aliases());
		setColumnWidth(aliases, 200, Unit.PX);
	}

	private IEditHandler<ItemValue<Domain>> createEditHandler() {
		return new IEditHandler<ItemValue<Domain>>() {

			@Override
			public SelectAction edit(CellPreviewEvent<ItemValue<Domain>> cpe) {
				if (cpe.getColumn() == 0) {
					return SelectAction.TOGGLE;
				} else {
					ItemValue<Domain> domain = cpe.getValue();
					Map<String, String> params = new HashMap<>();
					params.put(DomainKeys.domainUid.name(), domain.uid);
					Actions.get().showWithParams2("editDomain", params);
					return SelectAction.IGNORE;
				}
			}
		};
	}

	private ProvidesKey<ItemValue<Domain>> createKeyProvider() {
		return new ProvidesKey<ItemValue<Domain>>() {
			@Override
			public Object getKey(ItemValue<Domain> item) {
				if (item == null) {
					return null;
				}
				return item.uid;
			}
		};
	}

	public void setValues(List<ItemValue<Domain>> entities) {
		ldp.setList(entities);
		ldp.refresh();
	}

	public List<ItemValue<Domain>> getValues() {
		return ldp.getList();
	}

	public void refresh() {
		ldp.refresh();
	}

	public void selectAll(boolean checked) {
		for (ItemValue<Domain> s : getValues()) {
			selectionModel.setSelected(s, checked);
		}
	}

	public HandlerRegistration addSelectionChangeHandler(Handler handler) {
		return selectionModel.addSelectionChangeHandler(handler);
	}

	public Collection<ItemValue<Domain>> getSelected() {
		return selectionModel.getSelectedSet();
	}

	@Override
	public ProvidesKey<ItemValue<Domain>> getKeyProvider() {
		return keyProvider;
	}

	public void clearSelectionModel() {
		selectionModel.clear();
	}

}
