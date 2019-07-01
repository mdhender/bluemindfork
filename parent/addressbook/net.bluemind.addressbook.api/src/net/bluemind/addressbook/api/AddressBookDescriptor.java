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

import java.util.Collections;
import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class AddressBookDescriptor {

	public String name;
	public String domainUid;
	/**
	 * {@link DirEntry}
	 */
	public String owner;
	public boolean system;
	public Map<String, String> settings = Collections.emptyMap();
	public String orgUnitUid;

	public Long expectedId;

	public static AddressBookDescriptor create(String name, String owner, String domainUid) {
		AddressBookDescriptor ab = new AddressBookDescriptor();
		ab.name = name;
		ab.owner = owner;
		ab.domainUid = domainUid;
		return ab;
	}

	public static AddressBookDescriptor create(String name, String owner, String domainUid,
			Map<String, String> settings) {
		AddressBookDescriptor ab = new AddressBookDescriptor();
		ab.name = name;
		ab.owner = owner;
		ab.domainUid = domainUid;
		ab.settings = settings;
		return ab;
	}

	public static AddressBookDescriptor createSystem(String name, String owner, String domainUid) {
		AddressBookDescriptor ab = new AddressBookDescriptor();
		ab.name = name;
		ab.owner = owner;
		ab.domainUid = domainUid;
		ab.system = true;
		return ab;
	}
}
