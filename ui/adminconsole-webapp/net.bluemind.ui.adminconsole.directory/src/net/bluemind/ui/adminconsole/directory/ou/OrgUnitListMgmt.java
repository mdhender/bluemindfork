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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Tree;

import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitCheckBox;
import net.bluemind.ui.adminconsole.directory.ou.model.OrgUnitItem;
import net.bluemind.ui.common.client.SizeHint;

public class OrgUnitListMgmt {

	public static final EventBus CHECK_EVENT_BUS = GWT.create(SimpleEventBus.class);
	public static final EventBus RESOURCES_BUS = GWT.create(SimpleEventBus.class);
	public static final EventBus ROLE_DETAIL_BUS = GWT.create(SimpleEventBus.class);

	private static OrgUnitListMgmt instance;
	Tree tree;

	enum TreeAction {
		CREATE, UPDATE, DELETE;
	}

	public static OrgUnitListMgmt get() {
		if (instance == null) {
			instance = new OrgUnitListMgmt();
		}
		return instance;
	}

	public Collection<OrgUnitItem> getItems() {
		Collection<OrgUnitItem> items = new ArrayList<>();
		if (tree != null) {
			tree.treeItemIterator().forEachRemaining(i -> items.add((OrgUnitItem) i));
		}
		return items;
	}

	public List<OrgUnitItem> getSelectedItems() {
		return getItems().stream().filter(i -> ((OrgUnitCheckBox) i.getWidget()).getValue())
				.sorted(Comparator.comparingInt(OrgUnitItem::getPathDepth)).collect(Collectors.toList());
	}

	public List<OrgUnitItem> getSelectedEnabledItems() {
		return getItems().stream().filter(
				i -> ((OrgUnitCheckBox) i.getWidget()).isEnabled() && ((OrgUnitCheckBox) i.getWidget()).getValue())
				.collect(Collectors.toList());
	}

	public boolean hasSelectedItems() {
		return getItems().stream().anyMatch(i -> ((OrgUnitCheckBox) i.getWidget()).getValue());
	}

	static DialogBox createDialog(final OUCreateEditDialog ied) {
		SizeHint sh = ied.getSizeHint();
		final DialogBox os = new DialogBox();

		os.addStyleName("dialog");
		ied.setSize(sh.getWidth() + "px", sh.getHeight() + "px");
		ied.setOverlay(os);
		os.setWidget(ied);
		os.setGlassEnabled(true);
		os.setAutoHideEnabled(false);
		os.setGlassStyleName("modalOverlay");
		os.setModal(false);
		os.center();
		os.show();

		return os;
	}

	Tree copyTree() {
		Tree newTree = new Tree();
		tree.treeItemIterator().forEachRemaining(newTree::addItem);
		return newTree;
	}

	static List<OrgUnitPath> getParents(OrgUnitPath p) {
		OrgUnitPath parent = p.parent;
		if (parent == null) {
			return Arrays.asList(p);
		}

		List<OrgUnitPath> parents = new ArrayList<>();
		while (parent != null) {
			parents.add(parent);
			parent = parent.parent;
		}

		return parents;
	}

}
