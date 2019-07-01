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
package net.bluemind.ui.gwtrole.client.internal;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.role.api.gwt.js.JsRoleDescriptor;

public class UIRole {
	public UIRole(JsRoleDescriptor desc) {
		this.role = desc;
		childs = new ArrayList<>();
	}

	public JsRoleDescriptor role;
	public List<UIRole> childs;

	public boolean addChildRole(JsRoleDescriptor desc) {
		if (role.getId().equals(desc.getParentRoleId())) {
			childs.add(new UIRole(desc));
			return true;
		} else {
			for (UIRole ccc : childs) {
				if (ccc.addChildRole(desc)) {
					return true;
				}
			}
			return false;
		}
	}

	public String getLabel() {
		return role.getLabel();
	}

	public String getDescription() {
		return role.getDescription();
	}
}