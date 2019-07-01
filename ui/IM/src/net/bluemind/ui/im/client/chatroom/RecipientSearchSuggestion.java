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
package net.bluemind.ui.im.client.chatroom;

import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;

public class RecipientSearchSuggestion implements Suggestion {

	private ItemValue<DirEntry> contact;

	public RecipientSearchSuggestion(ItemValue<DirEntry> contact) {
		this.contact = contact;
	}

	@Override
	public String getDisplayString() {
		return contact.displayName + " <" + contact.value.email + ">";
	}

	@Override
	public String getReplacementString() {
		return contact.value.email;
	}

	public ItemValue<DirEntry> getContact() {
		return contact;
	}

}
