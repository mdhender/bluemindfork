/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.addressbook.adapter;

import net.bluemind.directory.api.BaseDirEntry;

public class AddressbookOwner {
	public final String domainUid;
	public final String userUid;
	public final BaseDirEntry.Kind kind;

	public AddressbookOwner(String domainUid, String userUid, BaseDirEntry.Kind kind) {
		this.domainUid = domainUid;
		this.userUid = userUid;
		this.kind = kind;
	}
}
