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
package net.bluemind.directory.hollow.datamodel.consumer;

import java.util.Collection;
import java.util.Optional;

public class DefaultDirectorySearch implements SerializedDirectorySearch {

	protected final DirectoryDeserializer deserializer;

	public DefaultDirectorySearch(DirectoryDeserializer deserializer) {
		this.deserializer = deserializer;
	}

	@Override
	public Optional<OfflineAddressBook> root() {
		return deserializer.root();
	}

	@Override
	public Optional<AddressBookRecord> byDistinguishedName(String distinguishedName) {
		return deserializer.byDistinguishedName(distinguishedName);
	}

	@Override
	public Optional<AddressBookRecord> byUid(String uid) {
		return deserializer.byUid(uid);
	}

	@Override
	public Optional<AddressBookRecord> byMinimalId(long minimalId) {
		return deserializer.byMinimalId(minimalId);
	}

	@Override
	public Optional<AddressBookRecord> byEmail(String email) {
		return deserializer.byEmail(email);
	}

	@Override
	public Collection<AddressBookRecord> all() {
		return deserializer.all();
	}

}
