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
package net.bluemind.ui.adminconsole.directory.ou.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TreeItem;

import net.bluemind.directory.api.OrgUnitPath;
import net.bluemind.ui.adminconsole.directory.ou.OrgUnitStyle;
import net.bluemind.ui.adminconsole.directory.ou.OrgUnitStyle.Style;

public class OrgUnitItem extends TreeItem {

	public OrgUnitPath path;
	private List<OrgUnitPath> pathChildrens = new LinkedList<>();
	private String uid;
	private boolean root;
	private Style s;
	private Long itemId;

	public OrgUnitItem(OrgUnitPath path) {
		this.path = path;
		this.uid = path.uid;
		this.root = false;

		s = OrgUnitStyle.getOrgUnitStyle();
		s.ensureInjected();
		addStyleName(s.itemTree());
	}

	public void loadChildren() {
		loadPathChildrens();
		addChildrenTreeItems();
	}

	public int getPathNbChildren() {
		return pathChildrens.size();
	}

	private void loadPathChildrens() {
		if (path.parent == null) {
			return;
		}
		for (OrgUnitPath p = path; p != null; p = p.parent) {
			if (!p.uid.equals(getRootUid())) {
				pathChildrens.add(p);
			}
		}
		Collections.reverse(pathChildrens);
	}

	public static String getPathName(OrgUnitPath path) {
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

	public String getPathName() {
		if (root) {
			return getRootName();
		}
		return getPathName(path);
	}

	public int getPathDepth() {
		return getPathName().split("/").length;
	}

	public String getRootName() {
		return getPathName(path).split("/")[0];
	}

	public String getLastChildName() {
		return getPathName(path).split("/")[path.path().size() - 1];
	}

	public String getRootUid() {
		return path.path().get(path.path().size() - 1);
	}

	private void addChildrenTreeItems() {
		OrgUnitItem itemParent = this;
		for (OrgUnitPath child : pathChildrens) {
			if (child != null && !child.uid.equals(getRootUid())) {
				OrgUnitItem item = new OrgUnitItem(child);
				itemParent.addItem(item);
				itemParent = item;
			}
		}
	}

	public void updateRoot() {
		if (getParentItem() == null && getPathDepth() > 1) {
			uid = getRootUid();
			root = true;
		}
	}

	public void orderingItemChildren() {
		List<OrgUnitItem> children = new ArrayList<>();
		for (int i = 0; i < getChildCount(); i++) {
			OrgUnitItem child = (OrgUnitItem) getChild(i);
			if (child.getChildCount() > 0) {
				child.orderingItemChildren();
			}
			children.add(child);
		}
		removeItems();
		children.stream().sorted(Comparator.comparing(OrgUnitItem::getName)).forEach(this::addItem);
	}

	public void getItemChildren(List<OrgUnitItem> childrenList) {
		int nbChildren = getChildCount();
		for (int i = 0; i < nbChildren; i++) {
			OrgUnitItem child = (OrgUnitItem) getChild(i);
			childrenList.add(child);
			child.getItemChildren(childrenList);
		}
	}

	public void toogleHierarchy(Boolean checked) {
		((CheckBox) getWidget()).setValue(checked, false);
		toggleChildren(checked);
	}

	private void toggleChildren(Boolean checked) {
		int nbChildren = getChildCount();
		for (int i = 0; i < nbChildren; ++i) {
			OrgUnitItem childItem = (OrgUnitItem) getChild(i);
			CheckBox checkBox = (CheckBox) childItem.getWidget();
			checkBox.setValue(checked, false);
			checkBox.setEnabled(!checked);
			if (childItem.getChildCount() > 0) {
				childItem.toggleChildren(checked);
			}
		}
	}

	public OrgUnitCheckBox createCheckBox() {
		setWidget(new OrgUnitCheckBox(this));
		OrgUnitCheckBox widget = (OrgUnitCheckBox) getWidget();
		widget.addStyleName(getCheckBoxLabel());
		return widget;
	}

	public String getCheckBoxLabel() {
		return s.checkboxLabel();
	}

	public String getUid() {
		return uid;
	}

	public String getName() {
		if (root) {
			return getRootName();
		}
		return path.name;
	}

	public String getParentUid() {
		String parentUid = "";
		if (getParentItem() != null) {
			parentUid = ((OrgUnitItem) getParentItem()).getUid();
		}
		return parentUid;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	@Override
	public String toString() {
		String item = "item " + getName() + " with uid " + getUid() + " and path " + path.path() + " and parentUID "
				+ getParentUid();
		if (path.parent != null) {
			item = item.concat(" and parent path uid " + path.parent.uid + " and parent path name " + path.parent.name);
		}
		return item;
	}

	public boolean isRoot() {
		return root;
	}
}
