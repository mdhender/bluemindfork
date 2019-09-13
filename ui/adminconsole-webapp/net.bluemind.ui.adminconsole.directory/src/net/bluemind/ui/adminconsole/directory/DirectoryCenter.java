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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.DirEntryQuery.Dir;
import net.bluemind.directory.api.DirEntryQuery.OrderBy;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.domain.api.Domain;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.IDomainChangedListener;
import net.bluemind.ui.adminconsole.base.SubscriptionInfoHolder;
import net.bluemind.ui.adminconsole.base.ui.ACSimplePager;
import net.bluemind.ui.adminconsole.base.ui.MenuButton.PopupOrientation;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;
import net.bluemind.ui.adminconsole.directory.l10n.DirectoryCenterConstants;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwttask.client.TaskWatcher;
import net.bluemind.user.api.IUserPromise;
import net.bluemind.user.api.gwt.endpoint.UserGwtEndpoint;

public class DirectoryCenter extends Composite implements IGwtScreenRoot, IDomainChangedListener {

	public interface BBBundle extends ClientBundle {
		@Source("DirectoryCenter.css")
		BBStyle getStyle();
	}

	public interface BBStyle extends CssResource {
		String newButton();
	}

	public static final BBBundle bundle;
	public static final BBStyle style;

	static {
		bundle = GWT.create(BBBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();
	}

	interface BMDataGridResources extends DataGrid.Resources {
		@Override
		@Source({ DataGrid.Style.DEFAULT_CSS, "BMDataGrid.css" })
		DataGrid.Style dataGridStyle();
	}

	protected static final BMDataGridResources dataGridRes = GWT.create(BMDataGridResources.class);

	private DataGrid.Style bmDataGridStyle;

	private static DirectoryCenterUiBinder uiBinder = GWT.create(DirectoryCenterUiBinder.class);

	interface DirectoryCenterUiBinder extends UiBinder<DockLayoutPanel, DirectoryCenter> {

	}

	private static final DirectoryCenterConstants constants = DirectoryCenterConstants.INST;

	public static final int PAGE_SIZE = 25;
	public static final String TYPE = "bm.ac.DirectoryBrowser";

	@UiField
	DirectoryCenterGrid grid;

	@UiField(provided = true)
	SimplePager pager;

	@UiField
	TextBox search;

	@UiHandler("search")
	void searchOnKeyPress(KeyPressEvent event) {
		if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
			DirectoryState.state(DomainsHolder.get().getSelectedDomain().uid).setSearch(search.getText());
			pager.firstPage();
			find();
		}
	}

	@UiField
	Button deleteButton;

	@UiHandler("deleteButton")
	void deleteClick(ClickEvent e) {
		Collection<ItemValue<DirEntry>> selection = grid.getSelected();
		List<ItemValue<DirEntry>> listSelection = new ArrayList<ItemValue<DirEntry>>(selection);

		GWT.log("Selection Size = " + selection.size());

		String confirm = constants.deleteConfirmation();
		if (selection.size() > 1) {
			confirm = constants.massDeleteConfirmation();
		}

		if (Window.confirm(confirm)) {
			DirectoryGwtEndpoint dir = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(),
					DomainsHolder.get().getSelectedDomain().uid);
			doDelete(listSelection, dir);
		}
	}

	private void doDelete(List<ItemValue<DirEntry>> listSelection, DirectoryGwtEndpoint dir) {
		if (listSelection.isEmpty()) {
			find();
			grid.clearSelectionModel();
			return;
		}
		ItemValue<DirEntry> entry = listSelection.remove(0);
		dir.deleteByEntryUid(entry.value.entryUid, new AsyncHandler<TaskRef>() {

			@Override
			public void failure(Throwable e) {
			}

			@Override
			public void success(TaskRef value) {
				CompletableFuture<Void> status = TaskWatcher.track(value.id, true);
				status.thenRun(() -> {
					doDelete(listSelection, dir);
				});
			}
		});
	}

	@UiField
	ToggleButton userFilter;

	@UiHandler("userFilter")
	void ueventClick(ClickEvent e) {
		pager.firstPage();
		DirectoryState.state(DomainsHolder.get().getSelectedDomain().uid).setUserFilter(userFilter.getValue());
		find();
	}

	@UiField
	ToggleButton groupFilter;

	@UiHandler("groupFilter")
	void geventClick(ClickEvent e) {
		pager.firstPage();
		DirectoryState.state(DomainsHolder.get().getSelectedDomain().uid).setGroupFilter(groupFilter.getValue());
		find();
	}

	@UiField
	ToggleButton resourceFilter;

	@UiHandler("resourceFilter")
	void reventClick(ClickEvent e) {
		pager.firstPage();
		DirectoryState.state(DomainsHolder.get().getSelectedDomain().uid).setResourceFilter(resourceFilter.getValue());
		find();
	}

	@UiField
	ToggleButton mailshareFilter;

	@UiHandler("mailshareFilter")
	void meventClick(ClickEvent e) {
		pager.firstPage();
		DirectoryState.state(DomainsHolder.get().getSelectedDomain().uid)
				.setMailshareFilter(mailshareFilter.getValue());
		find();
	}

	@UiField
	ToggleButton externalUserFilter;

	@UiHandler("externalUserFilter")
	void eueventClick(ClickEvent e) {
		pager.firstPage();
		DirectoryState.state(DomainsHolder.get().getSelectedDomain().uid)
				.setExternalUserFilter(externalUserFilter.getValue());
		find();
	}

	@UiField
	ToggleButton calendarFilter;

	@UiHandler("calendarFilter")
	void ceventClick(ClickEvent e) {
		pager.firstPage();
		DirectoryState.state(DomainsHolder.get().getSelectedDomain().uid).setCalendarFilter(calendarFilter.getValue());
		find();
	}

	@UiField
	ToggleButton abFilter;

	@UiHandler("abFilter")
	void aeventClick(ClickEvent e) {
		pager.firstPage();
		DirectoryState.state(DomainsHolder.get().getSelectedDomain().uid).setAbFilter(abFilter.getValue());
		find();
	}

	@UiField
	SimplePanel newButtonContainer;

	@UiField
	ListBox filterBox;

	@UiHandler("filterBox")
	void filterBoxChanged(ChangeEvent e) {
		pager.firstPage();
		find();
	}

	private ScreenRoot instance;

	@UiHandler("filterBox")
	void onChange(ChangeEvent event) {
		pager.firstPage();
		find();
	}

	protected DirectoryCenter(ScreenRoot instance) {
		this.instance = instance;
		bmDataGridStyle = dataGridRes.dataGridStyle();
		bmDataGridStyle.ensureInjected();

		// Create a Pager to control the table,
		ACSimplePager.Resources pagerResources = GWT.create(ACSimplePager.Resources.class);

		pager = new ACSimplePager(TextLocation.CENTER, pagerResources, false, 0, true, PAGE_SIZE);

		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		search.getElement().setAttribute("placeholder", constants.addFilter());

		NewButton newButton = new NewButton(constants.newButton(), PopupOrientation.DownRight);
		newButton.addStyleName("primary");
		newButton.addStyleName(style.newButton());
		newButtonContainer.add(newButton);

		Handler handler = new Handler() {
			@Override
			public void onSelectionChange(SelectionChangeEvent event) {
				Collection<ItemValue<DirEntry>> entries = grid.getSelected();
				deleteButton.setEnabled(!entries.isEmpty());
			}
		};
		grid.addSelectionChangeHandler(handler);

		// suspended index 0
		filterBox.addItem(DirectoryCenterConstants.INST.allMembers(), DirEntryQuery.StateFilter.All.name());
		// suspended index 1
		filterBox.addItem(DirectoryCenterConstants.INST.activeMembers(), DirEntryQuery.StateFilter.Active.name());
		// suspended index 2
		filterBox.addItem(DirectoryCenterConstants.INST.suspendedMembers(), DirEntryQuery.StateFilter.Archived.name());

		if (SubscriptionInfoHolder.subIncludesSimpleAccount()) {
			filterBox.addItem(DirectoryCenterConstants.INST.fullAccount(), DirEntry.AccountType.FULL.name());
			filterBox.addItem(DirectoryCenterConstants.INST.basicAccount(), DirEntry.AccountType.SIMPLE.name());

		}

		newButtonContainer.getElement().setId("directory-center-new");
		deleteButton.getElement().setId("directory-center-delete");
		userFilter.getElement().setId("directory-center-toggle-user");
		groupFilter.getElement().setId("directory-center-toggle-group");
		resourceFilter.getElement().setId("directory-center-toggle-resource");
		mailshareFilter.getElement().setId("directory-center-toggle-mailshare");
		externalUserFilter.getElement().setId("directory-center-toggle-externaluser");
		filterBox.getElement().setId("directory-center-suspended");
		search.getElement().setId("directory-center-search");

	}

	private void find() {

		grid.selectAll(false);
		AsyncDataProvider<ItemValue<DirEntry>> provider = new AsyncDataProvider<ItemValue<DirEntry>>() {

			@Override
			protected void onRangeChanged(HasData<ItemValue<DirEntry>> display) {
				DirEntryQuery dq = new DirEntryQuery();

				// BM-6350
				dq.hiddenFilter = false;
				if (filterBox.getSelectedValue() != null) {
					if (SubscriptionInfoHolder.subIncludesSimpleAccount() && filterBox.getSelectedIndex() > 2) {
						dq.accountTypeFilter = DirEntry.AccountType.valueOf(filterBox.getSelectedValue());
					} else {
						dq.stateFilter = DirEntryQuery.StateFilter.valueOf(filterBox.getSelectedValue());
					}
				}
				dq.order = DirEntryQuery.order(OrderBy.displayname, Dir.asc);

				// construct Sort
				final ColumnSortList sortList = grid.getColumnSortList();

				if (sortList.size() != 0) {
					ColumnSortInfo csi = sortList.get(0);
					if (csi.isAscending()) {
						dq.order.dir = Dir.asc;
					} else {
						dq.order.dir = Dir.desc;
					}

					if (csi.getColumn().equals(grid.getTypeColumn())) {
						dq.order.by = OrderBy.kind;
					} else if (csi.getColumn().equals(grid.getDisplayNameColumn())) {
						dq.order.by = OrderBy.displayname;
					}
				} else {
					sortList.push(grid.getDisplayNameColumn());
				}

				int page = pager.getPage();
				if (page >= pager.getPageCount()) {
					page = pager.getPageCount() - 1;
				}
				if (page < 0) {
					page = 0;
				}

				dq.from = page * PAGE_SIZE;
				dq.size = PAGE_SIZE;

				boolean ufilter = userFilter.getValue();
				boolean gfilter = groupFilter.getValue();
				boolean rfilter = resourceFilter.getValue();
				boolean mfilter = mailshareFilter.getValue();
				boolean eufilter = externalUserFilter.getValue();

				String filterField = search.getValue();
				if (!filterField.isEmpty()) {
					dq.nameOrEmailFilter = filterField;
				}

				dq.kindsFilter = new ArrayList<>(5);
				if (ufilter) {
					dq.kindsFilter.add(Kind.USER);
				}
				if (gfilter) {
					dq.kindsFilter.add(Kind.GROUP);
				}
				if (rfilter) {
					dq.kindsFilter.add(Kind.RESOURCE);
				}
				if (mfilter) {
					dq.kindsFilter.add(Kind.MAILSHARE);
				}
				if (eufilter) {
					dq.kindsFilter.add(Kind.EXTERNALUSER);
				}
				if (abFilter.getValue() == true) {
					dq.kindsFilter.add(Kind.ADDRESSBOOK);
				}
				if (calendarFilter.getValue()) {
					dq.kindsFilter.add(Kind.CALENDAR);
				}

				if (dq.kindsFilter.size() == 0) {
					dq.kindsFilter.add(Kind.USER);
					dq.kindsFilter.add(Kind.GROUP);
					dq.kindsFilter.add(Kind.RESOURCE);
					dq.kindsFilter.add(Kind.MAILSHARE);
					dq.kindsFilter.add(Kind.CALENDAR);
					dq.kindsFilter.add(Kind.ADDRESSBOOK);
					dq.kindsFilter.add(Kind.EXTERNALUSER);
				}

				dq.onlyManagable = true;
				final int start = display.getVisibleRange().getStart();

				doFind(dq, new DefaultAsyncHandler<ListResult<ItemValue<DirEntry>>>() {
					@Override
					public void success(ListResult<ItemValue<DirEntry>> result) {
						grid.clearSelectionModel();

						updateRowData(start, result.values);
						updateRowCount((int) result.total, true);

						ScreenShowRequest ssr = new ScreenShowRequest();
						ssr.put("userFilter", userFilter.getValue());
						ssr.put("groupFilter", groupFilter.getValue());
						ssr.put("mailshareFilter", mailshareFilter.getValue());
						ssr.put("externalUserFilter", externalUserFilter.getValue());
						ssr.put("resourceFilter", resourceFilter.getValue());
						ssr.put("search", search.getValue());

						RowSelectionEventManager<ItemValue<DirEntry>> rowSelectionEventManager = RowSelectionEventManager
								.<ItemValue<DirEntry>>createRowManager(ssr);

						grid.setSelectionModel(grid.getSelectionModel(), rowSelectionEventManager);

						pager.setDisplay(grid);
					}

				});
			}

		};
		provider.addDataDisplay(grid);
	}

	private void doFind(DirEntryQuery dq, AsyncHandler<ListResult<ItemValue<DirEntry>>> asyncHandler) {
		IDirectoryPromise dir = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(),
				DomainsHolder.get().getSelectedDomain().uid).promiseApi();

		IUserPromise user = new UserGwtEndpoint(Ajax.TOKEN.getSessionId(), DomainsHolder.get().getSelectedDomain().uid)
				.promiseApi();
		CompletableFuture<ListResult<ItemValue<DirEntry>>> dirSearch = dir.search(dq);
		CompletableFuture<ListResult<ItemValue<DirEntry>>> userSearch = new CompletableFuture<>();
		if (dq.nameOrEmailFilter != null && dq.kindsFilter.contains(Kind.USER)) {
			user.byLogin(dq.nameOrEmailFilter).thenAccept(u -> {
				if (u == null) {
					userSearch.complete(new ListResult<>());
				} else {
					dir.findByEntryUid(u.uid).thenAccept(dirEntry -> {
						userSearch.complete(
								ListResult.create(Arrays.asList(ItemValue.create(dirEntry.entryUid, dirEntry))));
					});
				}
			});
		} else {
			userSearch.complete(new ListResult<>());
		}

		dirSearch.thenAccept(dirRet -> {
			userSearch.thenAccept(userRet -> {
				ListResult<ItemValue<DirEntry>> res = new ListResult<>();
				res.values = new ArrayList<>();
				res.values.addAll(dirRet.values);
				for (ItemValue<DirEntry> userItem : userRet.values) {
					boolean alreadyAdded = false;
					for (ItemValue<DirEntry> dirItem : dirRet.values) {
						if (userItem.uid.equals(dirItem.uid)) {
							alreadyAdded = true;
							break;
						}
					}
					if (!alreadyAdded) {
						res.values.add(userItem);
					}
				}
				res.total = res.values.size();
				CompletableFuture<ListResult<ItemValue<DirEntry>>> totalRes = null;
				if (res.total == 0) {
					// no result, maybe we are looking for a specific dirEntry via
					// its UID
					totalRes = dir.findByEntryUid(dq.nameOrEmailFilter).thenApply(dirEntry -> {
						if (dirEntry != null) {
							return ListResult.create(Arrays.asList(ItemValue.create(dirEntry.entryUid, dirEntry)));
						} else {
							return res;
						}
					});
				} else {
					totalRes = CompletableFuture.completedFuture(res);
				}
				totalRes.thenAccept(r -> {
					asyncHandler.success(r);
				}).exceptionally(t -> {
					asyncHandler.failure(t);
					return null;
				});
			});
		});
	}

	public void initGrid() {
		find();
	}

	protected void onScreenShown() {
		DirectoryState state = DirectoryState.state(DomainsHolder.get().getSelectedDomain().uid);
		userFilter.setValue(state.isUserFilter());

		groupFilter.setValue(state.isGroupFilter());
		resourceFilter.setValue(state.isResourceFilter());

		mailshareFilter.setValue(state.isMailshareFilter());
		abFilter.setValue(state.isAbFilter());
		calendarFilter.setValue(state.isCalendarFilter());
		search.setValue(state.getSearch());

		initGrid();
	}

	@Override
	public void activeDomainChanged(ItemValue<Domain> newActiveDomain) {
		GWT.log("active domain is now " + newActiveDomain.displayName);
		pager.firstPage();
		find();
	}

	@UiFactory
	public IconTips tips() {
		return IconTips.INST;
	}

	@Override
	public void attach(Element elt) {
		DOM.appendChild(elt, getElement());
		onScreenShown();
		onAttach();
	}

	@Override
	public void loadModel(JavaScriptObject model) {

	}

	@Override
	public void saveModel(JavaScriptObject model) {

	}

	@Override
	public void doLoad(ScreenRoot instance) {

	}

	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new DirectoryCenter(screenRoot);
			}
		});
		GWT.log("bm.ac.DirectoryBrowser registred");
	}

}
