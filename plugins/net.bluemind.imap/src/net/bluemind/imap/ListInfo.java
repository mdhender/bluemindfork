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
package net.bluemind.imap;

public class ListInfo {

	private String name;

	private boolean selectable;

	public ListInfo(String name, boolean selectable) {
		super();
		this.name = name;
		this.selectable = selectable;
	}

	public String getName() {
		return name;
	}

	public boolean isSelectable() {
		return selectable;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	@Override
	public String toString() {
		return name + (selectable ? " (s)" : "");
	}

}
