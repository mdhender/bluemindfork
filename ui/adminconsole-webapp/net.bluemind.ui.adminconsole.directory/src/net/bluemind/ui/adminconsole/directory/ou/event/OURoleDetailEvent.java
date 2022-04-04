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
package net.bluemind.ui.adminconsole.directory.ou.event;

import com.google.gwt.event.shared.GwtEvent;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;

public class OURoleDetailEvent extends GwtEvent<OURoleDetailEventHandler> {

	public static Type<OURoleDetailEventHandler> TYPE = new Type<>();

	public ItemValue<DirEntry> itemValue;

	public OURoleDetailEvent(ItemValue<DirEntry> itemValue) {
		this.itemValue = itemValue;
	}

	@Override
	public Type<OURoleDetailEventHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(OURoleDetailEventHandler handler) {
		handler.onRoleSelected(this);
	}
}
