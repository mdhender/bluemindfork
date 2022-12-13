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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.HasDirection.Direction;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Tree;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectoryPromise;
import net.bluemind.directory.api.IOrgUnitsPromise;
import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.directory.api.OrgUnitQuery;
import net.bluemind.directory.api.gwt.endpoint.DirectoryGwtEndpoint;
import net.bluemind.directory.api.gwt.endpoint.OrgUnitsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.directory.ou.OrgUnitListMgmt.TreeAction;
import net.bluemind.ui.adminconsole.directory.ou.event.OUResourcesEvent;
import net.bluemind.ui.adminconsole.directory.ou.l10n.OrgUnitConstants;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitCheckBox;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitItem;
import net.bluemind.ui.common.client.forms.Ajax;

public class OrgUnitTreeGrid extends Grid implements IGwtWidgetElement {

	Tree ouTree;
	private OrgUnitListMgmt unitListMngt = OrgUnitListMgmt.get();
	CheckBox allOrgUnits;
	private List<OrgUnitPath> unitPathList;

	public OrgUnitTreeGrid() {
		super(3, 1);
		setSize("100%", "320px");

		ouTree = new Tree();
		ouTree.setAnimationEnabled(true);
		ScrollPanel treePanel = new ScrollPanel(ouTree);
		treePanel.setSize("100%", "320px");
		unitListMngt.tree = ouTree;

		allOrgUnits = new CheckBox();
		allOrgUnits.setText(OrgUnitConstants.INST.browse(), Direction.LTR);
		allOrgUnits.addStyleName(OrgUnitStyle.getOrgUnitStyle().checkboxTitle());
		allOrgUnits.setVisible(false);

		allOrgUnits.addValueChangeHandler(event -> selectAll(event.getValue()));

		setCellPadding(2);
		getRowFormatter().setVerticalAlign(1, HasVerticalAlignment.ALIGN_TOP);
		setWidget(0, 0, allOrgUnits);
		setWidget(1, 0, treePanel);
		setWidget(2, 0, null);
	}

	private void loadItemIds() {
		if (unitListMngt.getItems().isEmpty()) {
			return;
		}

		List<String> orgUnitUids = unitListMngt.getItems().stream().map(OrgUnitItem::getUid)
				.collect(Collectors.toList());

		DirEntryQuery dq = new DirEntryQuery();
		dq.hiddenFilter = false;
		dq.kindsFilter = java.util.Arrays.asList(Kind.ORG_UNIT);
		dq.entries = orgUnitUids;
		dq.onlyManagable = true;

		doFindOrgUnitDirEntries(dq, new DefaultAsyncHandler<ListResult<ItemValue<DirEntry>>>() {
			@Override
			public void success(ListResult<ItemValue<DirEntry>> result) {

				ouTree.treeItemIterator().forEachRemaining(i -> {
					OrgUnitItem item = (OrgUnitItem) i;
					Optional<ItemValue<DirEntry>> findFirst = result.values.stream()
							.filter(r -> r.uid.equals(item.getUid())).findFirst();
					if (findFirst.isPresent()) {
						item.setItemId(findFirst.get().item().id);
						result.values.remove(findFirst.get());
					}
				});
			}
		});

	}

	private static void doFindOrgUnitDirEntries(DirEntryQuery dq,
			DefaultAsyncHandler<ListResult<ItemValue<DirEntry>>> asyncHandler) {
		IDirectoryPromise dir = new DirectoryGwtEndpoint(Ajax.TOKEN.getSessionId(),
				DomainsHolder.get().getSelectedDomain().uid).promiseApi();

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

	private void createOuTree(String searchString) {
		IOrgUnitsPromise units = new OrgUnitsGwtEndpoint(Ajax.TOKEN.getSessionId(),
				DomainsHolder.get().getSelectedDomain().uid).promiseApi();
		OrgUnitQuery q = new OrgUnitQuery();
		q.query = searchString;
		q.managableKinds = new HashSet<>(Arrays.asList(Kind.ORG_UNIT));

		units.search(q) //
				.thenApply(result -> {
					unitPathList = new ArrayList<>(result);
					return createOrgUnitTreeList(orderResultList(result));
				}) //
				.thenAccept(result -> {
					allOrgUnits.setVisible(!result.isEmpty());
					result.forEach(ouTree::addItem);
					ouTree.treeItemIterator().forEachRemaining(i -> {
						OrgUnitItem item = (OrgUnitItem) i;
						item.createCheckBox();
						OrgUnitCheckBox cb = (OrgUnitCheckBox) item.getWidget();
						cb.addValueChangeHandler(event -> {
							unitListMngt.focusedItem = event.getValue().booleanValue() ? cb.getItem() : null;
							cb.getItem().toogleHierarchy(event.getValue());
							allOrgUnits
									.setValue(unitListMngt.getSelectedItems().size() == unitListMngt.getItems().size());
							reloadResources(unitListMngt.hasSelectedItems());
						});
					});
				}) //
				.thenRun(this::loadItemIds) //
				.exceptionally(t -> {
					Notification.get().reportError(t);
					return null;
				});
	}

	private static List<OrgUnitItem> createOrgUnitTreeList(Map<String, List<OrgUnitPath>> paths) {
		List<OrgUnitItem> finalList = new ArrayList<>();

		paths.entrySet().forEach(p -> {
			List<OrgUnitPath> pathsList = p.getValue();
			Optional<OrgUnitPath> rootNode = pathsList.stream().filter(r -> r.path().size() == 1).findFirst();
			if (rootNode.isPresent()) {
				OrgUnitItem rootItem = new OrgUnitItem(rootNode.get());
				rootItem.loadChildrenPath();
				rootItem.loadChildrenItem(
						pathsList.stream().filter(r -> r.path().size() > 1).collect(Collectors.toList()));
				finalList.add(rootItem);
			}
		});

		finalList.forEach(OrgUnitItem::orderingItemChildren);
		finalList.forEach(OrgUnitItem::updateRoot);
		Collections.sort(finalList, new Comparator<OrgUnitItem>() {
			@Override
			public int compare(OrgUnitItem arg0, OrgUnitItem arg1) {
				return arg0.getName().compareTo(arg1.getName());
			}
		});
		return finalList;
	}

	private void updateOuTree(List<OrgUnitPath> pathListToUpdate, TreeAction action, String search) {
		if (unitPathList == null) {
			unitPathList = new ArrayList<>();
		}

		switch (action) {
		case CREATE:
			unitPathList.addAll(pathListToUpdate);
			break;
		case UPDATE:
			pathListToUpdate.stream().forEach(onepath -> {
				unitPathList.stream().filter(p -> p.uid.equals(onepath.uid)).findAny().ifPresent(path -> {
					path.name = onepath.name;
					path.parent = onepath.parent;
				});
				unitPathList.stream().filter(p -> p.path().contains(onepath.uid))
						.forEach(child -> loopOnParent(child, onepath));
			});
			break;
		case DELETE:
			pathListToUpdate.stream().forEach(onepath -> unitPathList.removeIf(p -> p.uid.equals(onepath.uid)));
			break;
		default:
			return;
		}

		List<OrgUnitItem> createOrgUnitTreeList = createOrgUnitTreeList(orderResultList(new ArrayList<>(unitPathList)));
		allOrgUnits.setVisible(!createOrgUnitTreeList.isEmpty());
		Tree copy = unitListMngt.copyTree();
		ouTree.removeItems();
		createOrgUnitTreeList.forEach(ouTree::addItem);
		ouTree.treeItemIterator().forEachRemaining(i -> {
			OrgUnitItem item = (OrgUnitItem) i;
			item.createCheckBox();
			OrgUnitCheckBox cb = (OrgUnitCheckBox) item.getWidget();
			cb.addValueChangeHandler(event -> {
				cb.getItem().toogleHierarchy(event.getValue());
				allOrgUnits.setValue(unitListMngt.getSelectedItems().size() == unitListMngt.getItems().size());
				reloadResources(unitListMngt.hasSelectedItems());
			});

			keepPreviousState(copy, item, action == TreeAction.UPDATE || action == TreeAction.CREATE);
			if (action == TreeAction.CREATE) {
				openParentStateForNewChild(pathListToUpdate, item);
			}
		});
		copy.clear();
		loadItemIds();
	}

	private void loopOnParent(OrgUnitPath child, OrgUnitPath onepath) {
		OrgUnitPath parent = child.parent;
		while (parent != null) {
			if (parent.uid.equals(onepath.uid)) {
				parent.name = onepath.name;
			}
			parent = parent.parent;
		}
	}

	private void openParentStateForNewChild(List<OrgUnitPath> pathListToUpdate, OrgUnitItem item) {
		pathListToUpdate.stream()
				.filter(p -> p.uid.equals(item.getUid()) && item.getParentItem() != null
						&& item.getParentItem().getChildCount() == 1)
				.findAny().ifPresent(path -> item.getParentItem().setState(true));
	}

	private void keepPreviousState(Tree copy, OrgUnitItem item, boolean keepSelection) {
		copy.treeItemIterator().forEachRemaining(copyI -> {
			if (((OrgUnitItem) copyI).getUid().equals(item.getUid())) {
				item.setState(copyI.getState());
				if (keepSelection) {
					((OrgUnitCheckBox) item.getWidget()).setValue(((OrgUnitCheckBox) copyI.getWidget()).getValue());
					((OrgUnitCheckBox) item.getWidget()).setEnabled(((OrgUnitCheckBox) copyI.getWidget()).isEnabled());
				}
			}
		});
	}

	private static Map<String, List<OrgUnitPath>> orderResultList(List<OrgUnitPath> result) {
		List<OrgUnitPath> completeList = loadMissingParentNodes(result);

		Map<String, List<OrgUnitPath>> pathMap = new HashMap<>();
		completeList.stream().filter(r -> r.path().size() == 1).forEach(e -> pathMap.put(e.uid, new ArrayList<>()));

		for (OrgUnitPath orgUnitPath : completeList) {
			String key = orgUnitPath.path().get(orgUnitPath.path().size() - 1);
			if (pathMap.get(key) != null) {
				pathMap.get(key).add(orgUnitPath);
			}
		}

		pathMap.entrySet().stream().forEach(p -> {
			List<OrgUnitPath> sortedList = p.getValue().stream()
					.sorted(Comparator.comparing(path -> path.path().stream().collect(Collectors.joining("/"))))
					.collect(Collectors.toList());
			p.setValue(sortedList);
		});

		return pathMap;
	}

	private static List<OrgUnitPath> loadMissingParentNodes(List<OrgUnitPath> result) {
		List<OrgUnitPath> rootNodes = result.stream().filter(r -> r.path().size() == 1).collect(Collectors.toList());

		Set<OrgUnitPath> newResult = new HashSet<>();
		result.forEach(r -> {
			List<OrgUnitPath> parents = OrgUnitListMgmt.getParents(r);
			OrgUnitPath rootNode = parents.stream().filter(p -> p.path().size() == 1).findFirst().get();
			if (!rootNodes.contains(rootNode)) {
				newResult.add(rootNode);
				newResult.addAll(parents);
			}
		});

		result.addAll(newResult);
		return result.stream().distinct().collect(Collectors.toList());
	}

	@Override
	public void attach(Element parent) {
	}

	@Override
	public void detach() {
	}

	@Override
	public void show() {
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	public void reload(String searchString) {
		ouTree.clear();
		createOuTree(searchString);
	}

	public void reload(List<OrgUnitPath> list, TreeAction action, String search) {
		updateOuTree(list, action, search);
	}

	public void reloadResources(boolean hasSelectedItems) {
		OrgUnitListMgmt.RESOURCES_BUS.fireEvent(new OUResourcesEvent(hasSelectedItems));
	}

	public void selectAll(boolean b) {
		ouTree.treeItemIterator().forEachRemaining(i -> ((OrgUnitItem) i).toogleHierarchy(b));
		reloadResources(b);
	}
}
