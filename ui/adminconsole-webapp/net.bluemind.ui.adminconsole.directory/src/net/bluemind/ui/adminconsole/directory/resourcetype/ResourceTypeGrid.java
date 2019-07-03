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
package net.bluemind.ui.adminconsole.directory.resourcetype;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.resource.api.type.ResourceType;
import net.bluemind.ui.admin.client.forms.det.CellHeader;
import net.bluemind.ui.admin.client.forms.det.IBmGrid;
import net.bluemind.ui.admin.client.forms.det.IEditHandler;
import net.bluemind.ui.admin.client.forms.det.RowSelectionEventManager;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.directory.resourcetype.l10n.ResourceTypeConstants;

public class ResourceTypeGrid extends DataGrid<ResourceType>implements IBmGrid<ResourceType> {

	private MultiSelectionModel<ResourceType> selectionModel;
	private ProvidesKey<ResourceType> keyProvider;
	private ListDataProvider<ResourceType> ldp;
	private String loc;

	public ResourceTypeGrid() {
		loc = LocaleInfo.getCurrentLocale().getLocaleName();
		if (loc.length() > 2) {
			loc = loc.substring(0, 2);
		}

		keyProvider = new ProvidesKey<ResourceType>() {
			@Override
			public Object getKey(ResourceType item) {
				return (item == null) ? null : item.identifier;
			}
		};

		selectionModel = new MultiSelectionModel<ResourceType>(keyProvider);
		this.getElement().getStyle().setCursor(Cursor.POINTER);

		IEditHandler<ResourceType> editHandler = new IEditHandler<ResourceType>() {

			@Override
			public SelectAction edit(CellPreviewEvent<ResourceType> cpe) {
				if (cpe.getColumn() == 0) {
					return SelectAction.TOGGLE;
				} else {
					ResourceType rt = cpe.getValue();
					Map<String, String> map = new HashMap<>();
					map.put("resourceTypeId", rt.identifier);
					// FIXME
					map.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);

					Actions.get().showWithParams2("editResourceType", map);
					return SelectAction.IGNORE;
				}
			}
		};

		RowSelectionEventManager<ResourceType> rowSelectionEventManager = RowSelectionEventManager
				.<ResourceType> createRowManager(editHandler);

		setSelectionModel(selectionModel, rowSelectionEventManager);

		Column<ResourceType, Boolean> checkColumn = new Column<ResourceType, Boolean>(new CheckboxCell(true, false)) {
			@Override
			public Boolean getValue(ResourceType de) {
				return selectionModel.isSelected(de);
			}
		};
		Header<Boolean> selHead = new CellHeader<ResourceType>(new CheckboxCell(), this, null);
		addColumn(checkColumn, selHead, selHead);
		setColumnWidth(checkColumn, 40, Unit.PX);

		Column<ResourceType, SafeHtml> iconColumn = new Column<ResourceType, SafeHtml>(new SafeHtmlCell()) {

			@Override
			public SafeHtml getValue(ResourceType rt) {
				SafeHtmlBuilder builder = new SafeHtmlBuilder();
				String url = "/api/resources/" + DomainsHolder.get().getSelectedDomain().uid + "/type/" + rt.identifier
						+ "/icon?timestamp=" + System.currentTimeMillis();
				// FIXME
				// if (rt.getDefaultIcon() > 0) {
				// url = GWT.getModuleBaseURL() + "fetchDocument?id=" +
				// rt.getDefaultIcon();
				// } else {
				// url = Glyphicons.INST.cargo_2x().getSafeUri().asString();
				// }
				//
				// url = Glyphicons.INST.cargo_2x().getSafeUri().asString();
				String html = "<img src=\"" + url + "\" width=\"24px\" height=\"24px\" alt=\"avatar\"/>";
				builder.appendHtmlConstant(html);
				return builder.toSafeHtml();
			}
		};
		addColumn(iconColumn);
		setColumnWidth(iconColumn, 40, Unit.PX);
		iconColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

		TextColumn<ResourceType> labelColumn = new TextColumn<ResourceType>() {

			@Override
			public String getValue(ResourceType rt) {
				String[] labels = rt.label.split("\n");
				String l = null;
				for (String label : labels) {
					if (label.startsWith(loc + "::")) {
						String[] i18nLabel = label.split("::");
						l = i18nLabel[1];
					}
				}
				return rt.label;
			}
		};
		labelColumn.setSortable(true);
		addColumn(labelColumn, ResourceTypeConstants.INST.label(), ResourceTypeConstants.INST.label());
		setColumnWidth(labelColumn, 25.0, Unit.PCT);

		// FIXME
		// if (Ajax.TOKEN.isDomainGlobal()) {
		// TextColumn<ResourceType> domainColumn = new
		// TextColumn<ResourceType>() {
		//
		// @Override
		// public String getValue(ResourceType rt) {
		// return rt.getDomain().getName();
		// }
		// };
		// addColumn(domainColumn, ResourceTypeConstants.INST.domainColumn());
		// setColumnWidth(domainColumn, 25.0, Unit.PCT);
		// }

		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);

		ldp = new ListDataProvider<ResourceType>();
		ldp.addDataDisplay(this);

		setPageSize(ResourceTypeCenter.PAGE_SIZE);
	}

	public Collection<ResourceType> getSelected() {
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
		for (ResourceType d : getValues()) {
			selectionModel.setSelected(d, b);
		}
	}

	@Override
	public List<ResourceType> getValues() {
		return ldp.getList();
	}

	@Override
	public void setValues(List<ResourceType> values) {
		ldp.setList(values);
		ldp.refresh();
	}

}
