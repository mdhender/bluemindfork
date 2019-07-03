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
import net.bluemind.role.api.gwt.js.JsRolesCategory;

public class UICategory {
	public JsRolesCategory category;
	public List<UIRole> roles = new ArrayList<UIRole>();

	public boolean addRole(JsRoleDescriptor desc, boolean force) {
		if (desc.getParentRoleId() == null) {
			roles.add(new UIRole(desc));
			return true;
		}
		for (UIRole r : roles) {
			if (r.addChildRole(desc)) {
				return true;
			}
		}

		if (desc.getParentRoleId() != null && force) {
			roles.add(new UIRole(desc));
			return true;
		}
		return false;
	}

	public String getLabel() {
		return category.getLabel();
	}

	public String getId() {
		return category.getId();
	}

}