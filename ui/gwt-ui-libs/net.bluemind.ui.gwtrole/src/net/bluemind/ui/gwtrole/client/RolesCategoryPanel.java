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
package net.bluemind.ui.gwtrole.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasTreeItems;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Tree;
import com.google.gwt.user.client.ui.TreeItem;

import net.bluemind.role.api.gwt.js.JsRoleDescriptor;
import net.bluemind.ui.gwtrole.client.internal.UICategory;
import net.bluemind.ui.gwtrole.client.internal.UIRole;

public class RolesCategoryPanel extends Composite implements HasValueChangeHandlers<Void> {

	interface PanelUiBinder extends UiBinder<HTMLPanel, RolesCategoryPanel> {
	}

	private static PanelUiBinder uiBinder = GWT.create(PanelUiBinder.class);

	private final UICategory category;

	@UiField
	Label categoryTitle;

	@UiField
	Tree rolesTree;

	private Set<String> fromProfile;

	private final List<ValueChangeHandler<Void>> valueChangeHandlers = new ArrayList<>();

	private Set<String> readOnlyRoles;

	private Set<String> roles;

	public RolesCategoryPanel(UICategory cat) {
		initWidget(uiBinder.createAndBindUi(this));
		this.category = cat;
		categoryTitle.setText(cat.getLabel());

		addRoles(rolesTree, cat.roles);

	}

	private void addRoles(HasTreeItems root, List<UIRole> roles) {
		roles.sort((a, b) -> {
			int i = b.role.getPriority() - a.role.getPriority();
			return i != 0 ? i : a.role.getLabel().compareTo(b.role.getLabel());
		});

		for (UIRole role : roles) {
			CheckBox cb = new CheckBox(role.getLabel());
			final TreeItem r = new TreeItem(cb);
			r.setUserObject(role.role);
			cb.setTitle(role.getDescription());
			root.addItem(r);
			addRoles(r, role.childs);
			cb.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

				@Override
				public void onValueChange(ValueChangeEvent<Boolean> event) {
					changeItemValue(r, event.getValue());

				}
			});
		}

	}

	protected void changeItemValue(TreeItem r, boolean value) {
		if (value) {
			openParent(r);

			checkChild(r, true);
		} else {
			checkChild(r, false);
		}
	}

	private void openParent(TreeItem r) {
		if (r.getParentItem() != null) {
			TreeItem parent = r.getParentItem();
			parent.setState(true);
			openParent(parent);
		}
	}

	private void checkChild(TreeItem r, boolean checked) {
		for (int i = 0; i < r.getChildCount(); i++) {
			TreeItem c = r.getChild(i);
			JsRoleDescriptor rd = (JsRoleDescriptor) c.getUserObject();
			if (!fromProfile.contains(rd.getId())) {
				((CheckBox) c.getWidget()).setEnabled(!checked);
				((CheckBox) c.getWidget()).setValue(checked);
				checkChild(c, checked);
			}
		}

		ValueChangeEvent.fire(this, null);
	}

	public String getCategoryId() {
		return category.getId();
	}

	public Set<String> getRoles() {
		Set<String> ret = new HashSet<>();

		for (Iterator<TreeItem> it = rolesTree.treeItemIterator(); it.hasNext();) {

			TreeItem ttt = it.next();
			CheckBox cb = (CheckBox) ttt.getWidget();
			if (cb.getValue() == true) {
				JsRoleDescriptor rd = (JsRoleDescriptor) ttt.getUserObject();
				if (fromProfile.contains(rd.getId()) && !this.roles.contains(rd.getId())
						&& !this.readOnlyRoles.contains(rd.getId())) {
					continue;
				}
				ret.add(rd.getId());
			}
		}
		return ret;
	}

	public void stRoles(Set<String> roles, Set<String> fromProfile, Set<String> readOnlyRoles, boolean readOnly) {

		this.fromProfile = fromProfile;
		this.roles = roles;
		this.readOnlyRoles = readOnlyRoles;
		for (Iterator<TreeItem> it = rolesTree.treeItemIterator(); it.hasNext();) {
			TreeItem ttt = it.next();
			CheckBox cb = (CheckBox) ttt.getWidget();
			JsRoleDescriptor rd = (JsRoleDescriptor) ttt.getUserObject();
			if (roles.contains(rd.getId())) {
				cb.setValue(true);
				changeItemValue(ttt, true);
			} else {
				changeItemValue(ttt, false);
			}

			if (fromProfile.contains(rd.getId())) {
				cb.setEnabled(false);
				cb.setValue(true);
				changeItemValue(ttt, true);
			}

			if (readOnlyRoles.contains(rd.getId()) || readOnly) {
				cb.setEnabled(false);
			}
		}
	}

	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Void> handler) {
		valueChangeHandlers.add(handler);
		return new HandlerRegistration() {

			@Override
			public void removeHandler() {
				valueChangeHandlers.remove(handler);

			}
		};
	}

	public void fireEvent(GwtEvent<?> event) {
		if (event instanceof ValueChangeEvent) {
			for (ValueChangeHandler<Void> handler : valueChangeHandlers) {
				handler.onValueChange((ValueChangeEvent<Void>) event);
			}
		} else {
			super.fireEvent(event);
		}
	}
}
