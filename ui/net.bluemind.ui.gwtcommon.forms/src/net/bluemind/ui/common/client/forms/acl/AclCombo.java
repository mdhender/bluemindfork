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
package net.bluemind.ui.common.client.forms.acl;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;

import net.bluemind.core.container.model.acl.Verb;

public class AclCombo extends Composite {

	private static final AclConstants constants = GWT.create(AclConstants.class);

	private ListBox combo;
	private HashMap<String, Integer> items;

	public AclCombo(Map<String, String> values) {
		combo = new ListBox();
		items = new HashMap<String, Integer>();
		initValue(values);
		initWidget(combo);
	}

	public void setVerbs(Map<String, String> verbs) {
		combo.clear();
		items.clear();
		initValue(verbs);
	}

	private void initValue(Map<String, String> values) {
		if (values == null || values.size() == 0) {
			combo.addItem(constants.aclAccess(), "access");
			items.put("access", items.size());

			combo.addItem(constants.aclSendOnBehalf(), "send-on-behalf");
			items.put("read", items.size());

			combo.addItem(constants.aclRead(), "read");
			items.put("read", items.size());

			combo.addItem(constants.aclWrite(), "write");
			items.put("write", items.size());

			combo.addItem(constants.aclAdmin(), "admin");
			items.put("admin", items.size());
		} else {
			if (values.containsKey("access")) {
				combo.addItem(values.get("access"), "access");
				items.put("access", items.size());
			}
			if (values.containsKey("send-on-behalf")) {
				combo.addItem(values.get("send-on-behalf"), "send-on-behalf");
				items.put("send-on-behalf", items.size());
			}
			if (values.containsKey("read")) {
				combo.addItem(values.get("read"), "read");
				items.put("read", items.size());
			}
			if (values.containsKey("write")) {
				combo.addItem(values.get("write"), "write");
				items.put("write", items.size());
			}
			if (values.containsKey("admin")) {
				combo.addItem(values.get("admin"), "admin");
				items.put("admin", items.size());
			}
		}
	}

	public void setValue(Verb r) {
		if (r == Verb.All) {
			combo.setSelectedIndex(items.get("admin"));
		} else if (r == Verb.Write) {
			combo.setSelectedIndex(items.get("write"));
		} else if (r == Verb.Read) {
			combo.setSelectedIndex(items.get("read"));
		} else if (r == Verb.Invitation && items.containsKey("access")) {
			combo.setSelectedIndex(items.get("access"));
		} else if (r == Verb.SendOnBehalf && items.containsKey("send-on-behalf")) {
			combo.setSelectedIndex(items.get("send-on-behalf"));
		}
	}

	public Verb getValue() {
		return getRightFromValue(combo.getValue(combo.getSelectedIndex()));
	}

	/**
	 * @param selectedIndex
	 * @return
	 */
	private Verb getRightFromValue(String value) {
		Verb r = null;
		if (value.equals("send-on-behalf")) {
			r = Verb.SendOnBehalf;
		} else if (value.equals("access")) {
			r = Verb.Invitation; // FIXME
		} else if (value.equals("read")) {
			r = Verb.Read;
		} else if (value.equals("write")) {
			r = Verb.Write;
		} else if (value.equals("admin")) {
			r = Verb.All;
		}
		return r;
	}

	public void setEnable(Boolean enabled) {
		combo.setEnabled(enabled);
	}

	/**
	 * @param comboValues
	 * @param rights
	 * @return
	 */
	public boolean isValidValue(Verb rights) {
		for (String value : items.keySet()) {
			if (rights.equals(getRightFromValue(value))) {
				return true;
			}
		}
		return false;
	}
}
