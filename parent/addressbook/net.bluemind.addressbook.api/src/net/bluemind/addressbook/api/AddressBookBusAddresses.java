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
package net.bluemind.addressbook.api;

public class AddressBookBusAddresses {
	public static final String BASE_ADDRESS = "bm.addressbook.hook";

	public static final String CREATED = BASE_ADDRESS + ".all.created";
	public static final String UPDATED = BASE_ADDRESS + ".all.updated";
	public static final String DELETED = BASE_ADDRESS + ".all.deleted";
	public static final String CHANGED = BASE_ADDRESS + ".all.changed";

	public static String getChangedEventAddress(String containerUid) {
		return BASE_ADDRESS + "." + containerUid + ".changed";
	}
}
