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
package net.bluemind.directory.hollow.datamodel.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class DirectorySearchFactory {

	private static Map<String, DirectoryDeserializer> deserializers = new HashMap<>();

	public static SerializedDirectorySearch get(String domain) {
		return DirectorySearchFactory.get(domain, Optional.empty());
	}

	/**
	 * Filter hidden entities
	 * 
	 * @param domain
	 * @return SerializedDirectorySearch sorting out all hidden entities when
	 *         querying Hollow
	 */
	public static SerializedDirectorySearch getFiltered(String domain) {
		return DirectorySearchFactory.get(domain, Optional.of(rec -> !rec.getHidden()));
	}

	private static SerializedDirectorySearch get(String domain, Optional<Predicate<AddressBookRecord>> matcher) {
		DirectoryDeserializer deserializer = DirectorySearchFactory.deserializers.computeIfAbsent(domain,
				DirectoryDeserializer::new);
		if (!matcher.isPresent()) {
			return new DefaultDirectorySearch(deserializer);
		} else {
			return new FilteredDirectorySearch(deserializer, matcher.get());
		}
	}

	public static Map<String, DirectoryDeserializer> getDeserializers() {
		return DirectorySearchFactory.deserializers;
	}

	public static void reset() {
		DirectorySearchFactory.deserializers = new HashMap<>();
	}

}
