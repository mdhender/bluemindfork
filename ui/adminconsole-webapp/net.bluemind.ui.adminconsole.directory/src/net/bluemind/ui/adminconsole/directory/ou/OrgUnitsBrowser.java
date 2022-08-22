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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.TextBox;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.OrgUnit;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.Ajax;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.ui.ACSimplePager;
import net.bluemind.ui.adminconsole.directory.ou.OrgUnitListMgmt.TreeAction;
import net.bluemind.ui.adminconsole.directory.ou.event.OUCheckBoxEvent;
import net.bluemind.ui.adminconsole.directory.ou.event.OUCheckBoxEventHandler;
import net.bluemind.ui.adminconsole.directory.ou.event.OUResourcesEvent;
import net.bluemind.ui.adminconsole.directory.ou.event.OUResourcesEventHandler;
import net.bluemind.ui.adminconsole.directory.ou.event.OURoleDetailEvent;
import net.bluemind.ui.adminconsole.directory.ou.event.OURoleDetailEventHandler;
import net.bluemind.ui.adminconsole.directory.ou.l10n.OrgUnitConstants;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitItem;

public class OrgUnitsBrowser extends Composite
		implements IGwtScreenRoot, OUCheckBoxEventHandler, OUResourcesEventHandler, OURoleDetailEventHandler {

	private OrgUnitListMgmt unitListMngt = OrgUnitListMgmt.get();

	@UiField
	TextBox search;

	@UiHandler("search")
	void searchOnKeyPress(KeyPressEvent event) {
		if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
			unitGrid.reload(search.getValue());
		}
	}

	@UiField
	OrgUnitTreeGrid unitGrid;

	@UiField
	OrgResourceGrid resourceGrid;

	@UiField
	OrgAdminResourceGrid adminResourceGrid;

	@UiField
	TabLayoutPanel tabContainer;

	@UiField
	Label resourceDesc;

	@UiField
	Label adminResourceDesc;

	@UiField
	Button newButton;

	@UiField
	Button editButton;

	@UiField
	Button deleteButton;

	@UiField(provided = true)
	SimplePager pagerResource;

	@UiField(provided = true)
	SimplePager pagerAdminResource;

	@UiField
	OrgUnitsAdminRolesTree ouRolesTree;

	private ScreenRoot instance;
	private IOrgUnitsPromise orgUnitPromiseApi;
	private String domainUid;

	interface OrgUnitsCenterUiBinder extends UiBinder<DockLayoutPanel, OrgUnitsBrowser> {
	}

	private static OrgUnitsCenterUiBinder uiBinder = GWT.create(OrgUnitsCenterUiBinder.class);

	public static final String TYPE = "bm.ac.OrgUnitsBrowser";

	interface BMDataGridResources extends DataGrid.Resources {
		@Override
		@Source({ DataGrid.Style.DEFAULT_CSS, "OUDataGrid.css" })
		DataGrid.Style dataGridStyle();
	}

	protected static final BMDataGridResources dataGridRes = GWT.create(BMDataGridResources.class);

	private DataGrid.Style bmDataGridStyle;

	private OrgUnitsBrowser(ScreenRoot instance) {
		this.instance = instance;
		bmDataGridStyle = dataGridRes.dataGridStyle();
		bmDataGridStyle.ensureInjected();
		this.domainUid = DomainsHolder.get().getSelectedDomain().uid;

		ACSimplePager.Resources pagerResources = GWT.create(ACSimplePager.Resources.class);
		pagerResource = new ACSimplePager(TextLocation.CENTER, pagerResources, false, 0, true,
				CommonOrgResourceGrid.PAGE_SIZE);
		pagerAdminResource = new ACSimplePager(TextLocation.CENTER, pagerResources, false, 0, true,
				CommonOrgResourceGrid.PAGE_SIZE);

		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		OrgUnitListMgmt.CHECK_EVENT_BUS.addHandler(OUCheckBoxEvent.TYPE, this);
		OrgUnitListMgmt.RESOURCES_BUS.addHandler(OUResourcesEvent.TYPE, this);
		OrgUnitListMgmt.ROLE_DETAIL_BUS.addHandler(OURoleDetailEvent.TYPE, this);

		tabContainer.addSelectionHandler(event -> {
			if (event.getSelectedItem() == 0 || event.getSelectedItem() == 1) {
				ouRolesTree.clearRoles();
				unitGrid.reloadResources(unitListMngt.hasSelectedItems());
				if (event.getSelectedItem() == 0) {
					setResourceGridHeaderTitle();
				} else {
					setAdminResourceGridHeaderTitle();
				}
			}
		});

		search.getElement().setAttribute("placeholder", getTexts().addFilter());

		orgUnitPromiseApi = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(), domainUid).promiseApi();

		newButton.addClickHandler(event -> createOrgUnit());

		editButton.addClickHandler(event -> editOrgUnit(getItemToEdit()));
	}

	private void createOrgUnit() {
		final OUCreateEditDialog ied = new OUCreateEditDialog(domainUid);
		ied.setAction(new ScheduledCommand() {

			@Override
			public void execute() {
				ItemValue<OrgUnit> ouToSave = ied.getOUItem();
				orgUnitPromiseApi.create(ouToSave.uid, ouToSave.value) //
						.thenAccept(v -> orgUnitPromiseApi.getPath(ouToSave.uid) //
								.thenAccept(path -> reloadAfterAction(Arrays.asList(path), TreeAction.CREATE))) //
						.exceptionally(ex -> {
							Notification.get().reportError(ex.getMessage());
							return null;
						});
				;
			}
		});
		OrgUnitListMgmt.createDialog(ied);
	}

	private void editOrgUnit(final Optional<OrgUnitItem> item) {
		item.ifPresent(i -> {
			final OUCreateEditDialog ied = new OUCreateEditDialog(i);
			ied.setAction(new ScheduledCommand() {

				@Override
				public void execute() {
					ItemValue<OrgUnit> ouToSave = ied.getOUItem();
					orgUnitPromiseApi.update(ouToSave.uid, ouToSave.value) //
							.thenAccept(v -> orgUnitPromiseApi.getPath(ouToSave.uid) //
									.thenAccept(path -> reloadAfterAction(Arrays.asList(path), TreeAction.UPDATE))) //
							.exceptionally(ex -> {
								Notification.get().reportError(ex.getMessage());
								return null;
							});
				}
			});
			OrgUnitListMgmt.createDialog(ied);
		});
	}

	private Optional<OrgUnitItem> getItemToEdit() {
		List<OrgUnitItem> selectedEnabledItems = unitListMngt.getSelectedEnabledItems();
		if (selectedEnabledItems.isEmpty()) {
			return Optional.empty();
		}

		if (selectedEnabledItems.size() > 1) {
			editButton.setTitle(getTexts().forbiddenMultiEdition());
			return Optional.empty();
		}

		return Optional.of(selectedEnabledItems.get(0));
	}

	@UiHandler("deleteButton")
	void deleteClick(ClickEvent e) {
		List<OrgUnitItem> selection = unitListMngt.getSelectedItems();
		Collections.reverse(selection);

		String confirm = getTexts().deleteConfirmation();
		if (selection.size() > 1) {
			confirm = getTexts().massDeleteConfirmation();
		}

		List<OrgUnitItem> cannotBeDeleted = selection.stream()
				.filter(i -> resourceGrid.getValues().stream().anyMatch(r -> i.getUid().equals(r.value.orgUnitUid)))
				.collect(Collectors.toList());
		selection.removeAll(cannotBeDeleted);
		if (!cannotBeDeleted.isEmpty()) {
			// if delete list contains item with children present in cannot delete
			// => move item to cannot delete list
			selection.forEach(s -> {
				if (s != null) {
					List<OrgUnitItem> childrenList = new ArrayList<>();
					s.getItemChildren(childrenList);
					if (childrenList.stream()
							.anyMatch(c -> cannotBeDeleted.stream().anyMatch(i -> c.getUid().equals(i.getUid())))) {
						cannotBeDeleted.add(s);
						selection.remove(s);
					}
				}
			});
			if (selection.isEmpty()) {
				Notification.get().reportError(getTexts().forbiddenDeletion());
				return;
			}
			String toNotDelete = cannotBeDeleted.stream().map(OrgUnitItem::getName).collect(Collectors.joining(", "));
			String toDelete = selection.stream().map(OrgUnitItem::getName).collect(Collectors.joining(", "));
			confirm = getTexts().notDeletedConfirmation(toNotDelete, toDelete);
			if (cannotBeDeleted.size() > 1) {
				confirm = getTexts().notMassDeletedConfirmation(toNotDelete, toDelete);
			}
		}

		if (Window.confirm(confirm)) {
			List<String> listSelection = selection.stream().map(OrgUnitItem::getUid).collect(Collectors.toList());

			CompletableFuture<Void> toDelete = listSelection.stream() //
					.reduce(CompletableFuture.completedFuture(null), //
							(f, uid) -> f.thenCompose(v -> orgUnitPromiseApi.delete(uid)), //
							(f1, f2) -> f1.thenCompose(v -> f2));

			toDelete.thenRun(() -> {
				List<OrgUnitPath> toDeletePathList = selection.stream().filter(u -> !u.isRoot()).map(u -> u.path)
						.collect(Collectors.toList());
				selection.stream().filter(OrgUnitItem::isRoot).findFirst().ifPresent(u -> {
					OrgUnitPath path = new OrgUnitPath();
					path.uid = u.getRootUid();
					path.name = u.getRootName();
					toDeletePathList.add(path);
				});
				reloadAfterAction(toDeletePathList, TreeAction.DELETE);
			}).exceptionally(ex -> {
				Notification.get().reportError(ex.getMessage());
				return null;
			});
		}
	}

	private void reloadAfterAction(List<OrgUnitPath> list, TreeAction action) {
		unitGrid.reload(list, action, search.getValue());
		unitGrid.allOrgUnits.setValue(false);
		deleteButton.setEnabled(unitListMngt.hasSelectedItems());
		editButton.setEnabled(unitListMngt.hasSelectedItems() && getItemToEdit().isPresent());
	}

	protected void onScreenShown() {
		unitGrid.reload(search.getValue());
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
				return new OrgUnitsBrowser(screenRoot);
			}
		});
		GWT.log(TYPE + " registred");
	}

	private void setResourceGridHeaderTitle() {
		String resourcesTxt = null;
		int nbResourcesFound = resourceGrid.getValues().size();
		if (nbResourcesFound > 0) {
			List<String> ouNames = unitListMngt.getSelectedItems().stream().map(i -> i.getName())
					.collect(Collectors.toList());
			if (ouNames.size() == 1) {
				resourcesTxt = getTexts().resourceOuSelection(ouNames.get(0));
			} else {
				resourcesTxt = getTexts().massResourceOuSelection(String.valueOf(ouNames.size()));
			}
		}

		resourceDesc.setText(resourcesTxt);
	}

	private void setAdminResourceGridHeaderTitle() {
		String rolesTxt = null;
		int nbRolesFound = adminResourceGrid.getValues().size();
		if (nbRolesFound > 0 && unitListMngt.getSelectedItems().size() == 1) {
			rolesTxt = getTexts().roleOuSelection(unitListMngt.getSelectedItems().get(0).getName());
		}
		adminResourceDesc.setText(rolesTxt);
	}

	@Override
	public void onOuCheckBoxChanged(OUCheckBoxEvent checkedItemEvent) {
		deleteButton.setEnabled(checkedItemEvent.selectedItems);
		editButton.setEnabled(checkedItemEvent.selectedItems && getItemToEdit().isPresent());

		pagerResource.setVisible(resourceGrid.getRowCount() >= OrgResourceGrid.PAGE_SIZE);
		pagerResource.setDisplay(resourceGrid);
		setResourceGridHeaderTitle();

		pagerAdminResource.setVisible(adminResourceGrid.getRowCount() >= OrgResourceGrid.PAGE_SIZE);
		pagerAdminResource.setDisplay(adminResourceGrid);
		setAdminResourceGridHeaderTitle();

		ouRolesTree.clearRoles();
	}

	@Override
	public void onOuResourcesLoad(OUResourcesEvent resourcesEvent) {
		if (tabContainer.getSelectedIndex() == 0) {
			resourceGrid.loadResourceGridContent(resourcesEvent.selectedItems, pagerResource);
		} else if (tabContainer.getSelectedIndex() == 1) {
			adminResourceGrid.reload(pagerAdminResource);
		}
	}

	@Override
	public void onRoleSelected(OURoleDetailEvent roleClickEvent) {
		ouRolesTree.loadOuRoleTreeContext(roleClickEvent.itemValue.uid, domainUid, unitListMngt.focusedItem);
	}

	private OrgUnitConstants getTexts() {
		return OrgUnitConstants.INST;
	}

}
