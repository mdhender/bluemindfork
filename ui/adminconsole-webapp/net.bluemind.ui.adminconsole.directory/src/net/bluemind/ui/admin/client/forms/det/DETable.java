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

import java.util.Collection;
import java.util.Set;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.editor.ui.client.adapters.ValueBoxEditor;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.Range;

import net.bluemind.directory.api.DirEntry;
import net.bluemind.ui.adminconsole.directory.IconTips;

/**
 * {@link DirectoryEntry} paginated table.
 * 
 * 
 */
public class DETable extends DataGrid<DirEntry> {

	private MultiSelectionModel<DirEntry> selectionModel;
	private ProvidesKey<DirEntry> keyProvider;
	private DEDataProvider provider;

	public interface BBBundle extends ClientBundle {

		@Source("DETable.css")
		BBStyle getStyle();

	}

	public interface BBStyle extends CssResource {
		String typeColumn();
	}

	public static final BBBundle bundle;
	public static final BBStyle style;

	static {
		bundle = GWT.create(BBBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();
	}

	public static interface DET extends Constants {

		String selectColumn();

		String typeColumn();

		String loginColumn();

		String displayNameColumn();
	}

	private static final DET constants = GWT.create(DET.class);

	public DETable() {
		super();
		constructCellTable();
		setEmptyTableWidget(null);
		setLoadingIndicator(null);
		setPageSize(25);
		setHeight("100%");
		Window.addResizeHandler(new ResizeHandler() {

			@Override
			public void onResize(ResizeEvent event) {
				DETable.this.onResize();
			}
		});
	}

	public void initProvider(ValueBoxEditor<String> filter, Collection<DirEntry.Kind> entriesKind,
			SimpleBaseDirEntryFinder finder) {
		if (provider != null) {
			Set<HasData<DirEntry>> displays = provider.getDataDisplays();
			for (HasData<DirEntry> d : displays) {
				provider.removeDataDisplay(d);
			}
			provider = null;
		}

		provider = new DEDataProvider(this, selectionModel, entriesKind, filter, finder);
		provider.addDataDisplay(this);
		GWT.log("after provider.addDataDisplay(this)");
	}

	public void refresh() {
		Range range = new Range(0, DEDataProvider.PAGE_SIZE);
		setVisibleRangeAndClearData(range, true);
	}

	private void constructCellTable() {

		keyProvider = new ProvidesKey<DirEntry>() {
			@Override
			public Object getKey(DirEntry item) {
				return (item == null) ? null : item.entryUid;
			}
		};

		selectionModel = new MultiSelectionModel<DirEntry>(keyProvider);

		RowSelectionEventManager<DirEntry> rowSelectionEventManager = RowSelectionEventManager
				.<DirEntry>createRowManager();
		setSelectionModel(selectionModel, rowSelectionEventManager);

		Column<DirEntry, Boolean> checkColumn = new Column<DirEntry, Boolean>(new CheckboxCell(true, false)) {
			@Override
			public Boolean getValue(DirEntry de) {
				return selectionModel.isSelected(de);
			}

		};
		checkColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

		TextColumn<DirEntry> displayNameColumn = new TextColumn<DirEntry>() {

			@Override
			public String getValue(DirEntry de) {
				return de.displayName;
			}
		};

		Column<DirEntry, TippedResource> typeColumn = new Column<DirEntry, TippedResource>(new TooltipedImageCell()) {

			@Override
			public TippedResource getValue(DirEntry object) {
				String style = null;
				String tip = null;
				switch (object.kind) {
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
				default:
				case USER:
					style = "fa-user";
					tip = IconTips.INST.iconTipUser();
					break;
				}
				return new TippedResource(style, tip);
			}
		};
		typeColumn.setCellStyleNames(style.typeColumn());
		typeColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

		CellHeader<DirEntry> checkHeader = new CellHeader<DirEntry>(new CheckboxCell(), this, selectionModel);

		addColumn(checkColumn, checkHeader);
		setColumnWidth(checkColumn, 15, Unit.PX);

		addColumn(typeColumn, constants.typeColumn(), constants.typeColumn());
		setColumnWidth(typeColumn, 30, Unit.PX);

		addColumn(displayNameColumn, constants.displayNameColumn());
		setColumnWidth(displayNameColumn, 45.0, Unit.PCT);
	}

	public Set<DirEntry> getSelectedSet() {
		return selectionModel.getSelectedSet();
	}

	public void clearSelection() {
		selectionModel.clear();
	}

	public void exclude(Set<DirEntry> entities) {
		provider.exclude(entities);
	}

	public void include(Set<DirEntry> entities) {
		provider.include(entities);
	}
}
