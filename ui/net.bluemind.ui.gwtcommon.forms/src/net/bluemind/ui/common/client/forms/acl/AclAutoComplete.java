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

import java.util.Map;

import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.ui.common.client.forms.autocomplete.EntityEdit;
import net.bluemind.ui.common.client.forms.finder.AclAutoCompleteUserOrGroupFinder;

/**
 * UI-Binder doesn't want to rebind {@link EntityEdit} constructor using a
 * simple ui:with :
 * 
 * Returns class net.bluemind.ui.admin.client.forms.finder.UserFinder, can't be
 * used as interface net.bluemind.ui.admin.client.forms.IEntityFinder<T, TQ>
 * 
 * 
 */
public class AclAutoComplete extends AclAutoCompleteEntity {

	@UiConstructor
	public AclAutoComplete() {
		super(new AclAutoCompleteUserOrGroupFinder(10), false, null);
	}

	@Override
	public void setDescriptionText(String s) {
		// TODO Auto-generated method stub

	}

	@Override
	public Map<String, Widget> getWidgetsMap() {
		// TODO Auto-generated method stub
		return null;
	}

}
