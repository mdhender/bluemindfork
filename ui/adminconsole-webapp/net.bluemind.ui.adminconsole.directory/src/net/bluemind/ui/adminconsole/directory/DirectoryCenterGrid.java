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
package net.bluemind.ui.adminconsole.directory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.AsyncHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.admin.client.forms.det.CellHeader;
import net.bluemind.ui.admin.client.forms.det.IBmGrid;
import net.bluemind.ui.admin.client.forms.det.IEditHandler;
import net.bluemind.ui.admin.client.forms.det.RowSelectionEventManager;
import net.bluemind.ui.admin.client.forms.det.TippedResource;
import net.bluemind.ui.admin.client.forms.det.TooltipedImageCell;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.orgunit.OUUtils;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.adminconsole.directory.l10n.DirectoryCenterConstants;
import net.bluemind.ui.common.client.forms.Ajax;

public class DirectoryCenterGrid extends DataGrid<ItemValue<DirEntry>> implements IBmGrid<ItemValue<DirEntry>> {

	public interface DCGBundle extends ClientBundle {

		@Source("DirectoryCenterGrid.css")
		DCGStyle getStyle();

	}

	public interface DCGStyle extends CssResource {

		public String suspended();

	}

	public static DCGBundle bundle;
	public static DCGStyle style;

	private MultiSelectionModel<ItemValue<DirEntry>> selectionModel;
	private ProvidesKey<ItemValue<DirEntry>> keyProvider;
	private ListDataProvider<ItemValue<DirEntry>> ldp;
	private static final DirectoryCenterConstants constants = DirectoryCenterConstants.INST;
	private TextColumn<ItemValue<DirEntry>> emailColumn;
	private TextColumn<ItemValue<DirEntry>> displayNameColumn;
	private Column<ItemValue<DirEntry>, TippedResource> typeColumn;
	private TextColumn<ItemValue<DirEntry>> orgUnitColumn;

	public DirectoryCenterGrid() {
		bundle = GWT.create(DCGBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();
		keyProvider = new ProvidesKey<ItemValue<DirEntry>>() {
			@Override
			public Object getKey(ItemValue<DirEntry> item) {
				return (item == null) ? null : item.uid;
			}
		};
		this.getElement().getStyle().setCursor(Cursor.POINTER);
		selectionModel = new MultiSelectionModel<ItemValue<DirEntry>>(keyProvider) {

			@Override
			public void setSelected(ItemValue<DirEntry> item, boolean selected) {

				if (!item.value.system && !item.value.entryUid.equals(Ajax.TOKEN.getSubject())) {
					super.setSelected(item, selected);
				} else {
					// disable selection highlight
					super.setSelected(item, false);
				}
			}

		};

		IEditHandler<ItemValue<DirEntry>> editHandler = new IEditHandler<ItemValue<DirEntry>>() {

			@Override
			public SelectAction edit(CellPreviewEvent<ItemValue<DirEntry>> cpe) {
				if (cpe.getColumn() == 0) {
					return SelectAction.TOGGLE;
				} else {
					ItemValue<DirEntry> de = cpe.getValue();
					ScreenShowRequest ssr = new ScreenShowRequest();
					ssr.put("entry", de);

					// FIXME edit action ?!!!
					switch (de.value.kind) {
					case RESOURCE:
						// AdminCtrl.get().show("editResource", ssr);
						break;
					case USER:
						Map<String, String> params = new HashMap<>();
						params.put("userId", de.value.entryUid);
						params.put("domainUid", DomainsHolder.get().getSelectedDomain().uid);
						Actions.get().showWithParams2("editUser", params);
						break;
					case MAILSHARE:
						// AdminCtrl.get().show("editMailshare", ssr);
						break;
					case EXTERNALUSER:
						GWT.log("HELLO EDIT EXTERNALUSER");
						break;
					case GROUP:
						// AdminCtrl.get().show("editGroup", ssr);
						break;
					default:
						break;
					}
					return SelectAction.IGNORE;
				}
			}
		};

		RowSelectionEventManager<ItemValue<DirEntry>> rowSelectionEventManager = RowSelectionEventManager
				.<ItemValue<DirEntry>> createRowManager(editHandler);

		setSelectionModel(selectionModel, rowSelectionEventManager);

		Column<ItemValue<DirEntry>, Boolean> checkColumn = new Column<ItemValue<DirEntry>, Boolean>(
				new CheckboxCell(true, false)) {
			@Override
			public void render(Context context, ItemValue<DirEntry> object, SafeHtmlBuilder sb) {
				// prevent deleting system entities
				if (!object.value.system && Ajax.TOKEN.getRoles().contains(manageRole(object.value))) {
					// prevent deleting me
					if (!object.value.entryUid.equals(Ajax.TOKEN.getSubject())) {
						super.render(context, object, sb);
					}
				}
			}

			@Override
			public Boolean getValue(ItemValue<DirEntry> de) {
				return selectionModel.isSelected(de);
			}
		};

		CellHeader<ItemValue<DirEntry>> checkHeader = new CellHeader<ItemValue<DirEntry>>(new CheckboxCell(), this,
				selectionModel);

		addColumn(checkColumn, checkHeader);
		setColumnWidth(checkColumn, 40, Unit.PX);

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
		addColumn(typeColumn, constants.typeColumn());
		setColumnWidth(typeColumn, 60, Unit.PX);
		typeColumn.setSortable(true);

		Column<ItemValue<DirEntry>, SafeHtml> avatarColumn = new Column<ItemValue<DirEntry>, SafeHtml>(
				new SafeHtmlCell()) {

			@Override
			public SafeHtml getValue(ItemValue<DirEntry> object) {
				SafeHtmlBuilder builder = new SafeHtmlBuilder();
				if (object != null) {
					String html = "<img id=\"" + "res-icon-" + object.value.entryUid
							+ "\" src=\"\" width=\"24px\" height=\"24px\" alt=\"avatar\"/>";
					builder.appendHtmlConstant(html);
					loadIcon(object.value.entryUid);
				}
				return builder.toSafeHtml();
			}
		};
		addColumn(avatarColumn);
		setColumnWidth(avatarColumn, 60.0, Unit.PX);

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

		addColumn(displayNameColumn, constants.displayNameColumn());
		setColumnWidth(displayNameColumn, 20.0, Unit.PCT);
		displayNameColumn.setSortable(true);

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
		addColumn(emailColumn, constants.emailColumn());
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
		addColumn(orgUnitColumn, constants.orgUnitColumn());

		setHeight("100%");
		setEmptyTableWidget(null);
		setLoadingIndicator(null);

		setPageSize(DirectoryCenter.PAGE_SIZE);

		// add handler to sorting
		AsyncHandler columnSortHanler = new AsyncHandler(this);
		addColumnSortHandler(columnSortHanler);

		setRowStyles(new RowStyles<ItemValue<DirEntry>>() {
			@Override
			public String getStyleNames(ItemValue<DirEntry> row, int rowIndex) {

				return null;
			}
		});

		ldp = new ListDataProvider<ItemValue<DirEntry>>();
		ldp.addDataDisplay(this);

		RootLayoutPanel.get().add(this);
	}

	native String atob(String encoded)
	/*-{
    return atob(encoded);
	}-*/;

	private void loadIcon(final String rUid) {
		final String domainUid = DomainsHolder.get().getSelectedDomain().uid;
		new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).getEntryIcon(rUid,
				new DefaultAsyncHandler<byte[]>() {

					@Override
					public void success(byte[] value) {
						if (null != value) {
							String b64 = atob(new String(value));
							String imgSrc = "data:image/png;base64," + b64;
							Element elementById = DOM.getElementById("res-icon-" + rUid);
							elementById.setAttribute("src", imgSrc);
						}
					}

				});

	}

	protected String manageRole(DirEntry value) {
		switch (value.kind) {
		case ADDRESSBOOK:
			return BasicRoles.ROLE_MANAGE_DOMAIN_AB;
		case CALENDAR:
			return BasicRoles.ROLE_MANAGE_DOMAIN_CAL;
		case DOMAIN:
			return BasicRoles.ROLE_MANAGE_DOMAIN;
		case USER:
			return BasicRoles.ROLE_MANAGE_USER;
		case GROUP:
			return BasicRoles.ROLE_MANAGE_GROUP;
		case MAILSHARE:
			return BasicRoles.ROLE_MANAGE_MAILSHARE;
		case RESOURCE:
			return BasicRoles.ROLE_MANAGE_RESOURCE;
		case EXTERNALUSER:
			return BasicRoles.ROLE_MANAGE_EXTERNAL_USER;

		default:
			break;
		}
		return BasicRoles.ROLE_ADMIN;
	}

	public Collection<ItemValue<DirEntry>> getSelected() {
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
		for (ItemValue<DirEntry> d : getValues()) {
			selectionModel.setSelected(d, b);
		}
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

}
