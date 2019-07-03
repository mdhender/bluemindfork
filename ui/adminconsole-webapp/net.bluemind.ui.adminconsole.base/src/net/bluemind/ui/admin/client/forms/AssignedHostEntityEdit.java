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
package net.bluemind.ui.admin.client.forms;

import com.google.gwt.uibinder.client.UiConstructor;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.server.api.Server;
import net.bluemind.ui.common.client.forms.autocomplete.EntityEdit;
import net.bluemind.ui.common.client.forms.finder.ServerFinder;

/**
 * Finds hosts assigned to the current domain with the given tag
 * 
 * 
 */
public class AssignedHostEntityEdit extends EntityEdit<ItemValue<Server>, Void> {

	@UiConstructor
	public AssignedHostEntityEdit(boolean multival, String tagFilter) {
		super(new ServerFinder(tagFilter), multival, true, tagFilter);
	}

	public void setTagFilter(String tagFilter) {
		((ServerFinder) getFinder()).setTagFilter(tagFilter);
	}

	public void setDomainUid(String domainUid) {
		((ServerFinder) getFinder()).setDomain(domainUid);
		fillComboValues();
	}
}
