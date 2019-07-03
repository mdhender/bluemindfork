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

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.ui.common.client.forms.autocomplete.EntitySuggestion;

public class EntitySelectionHandler implements SelectionHandler<Suggestion> {

	private AclAutoCompleteEntity edit;

	public EntitySelectionHandler(AclAutoCompleteEntity eEdit) {
		this.edit = eEdit;
	}

	@Override
	public void onSelection(SelectionEvent<Suggestion> event) {
		@SuppressWarnings("unchecked")
		EntitySuggestion<DirEntry, DirEntryQuery> es = (EntitySuggestion<DirEntry, DirEntryQuery>) event
				.getSelectedItem();
		edit.select(es.getEntity());
	}
}
