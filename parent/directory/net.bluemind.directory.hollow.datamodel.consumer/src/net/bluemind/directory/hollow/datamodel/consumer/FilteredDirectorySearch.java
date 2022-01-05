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
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilteredDirectorySearch extends DefaultDirectorySearch implements BrowsableDirectorySearch {

	private final Predicate<AddressBookRecord> filter;

	public FilteredDirectorySearch(DirectoryDeserializer deserializer, Predicate<AddressBookRecord> filter) {
		super(deserializer);
		this.filter = filter;
	}

	@Override
	public Collection<AddressBookRecord> all() {
		return filter(deserializer.all());
	}

	@Override
	public List<AddressBookRecord> search(Query query) {
		return filter(deserializer.search(query));
	}

	@Override
	public List<AddressBookRecord> search(List<Predicate<? super AddressBookRecord>> predicates) {
		return filter(deserializer.search(predicates));
	}

	@Override
	public Collection<AddressBookRecord> byNameOrEmailPrefix(String value) {
		return filter(deserializer.byNameOrEmailPrefix(value));
	}

	@Override
	public SearchResults byKind(List<String> kinds, int offset, int limit) {
		return deserializer.byKind(kinds, offset, limit, filter);
	}

	private List<AddressBookRecord> filter(Collection<AddressBookRecord> records) {
		return records.stream().filter(filter).collect(Collectors.toList());
	}

}
