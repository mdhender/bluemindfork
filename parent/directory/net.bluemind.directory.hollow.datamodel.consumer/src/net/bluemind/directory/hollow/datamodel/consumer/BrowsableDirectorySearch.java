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

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public interface BrowsableDirectorySearch extends SerializedDirectorySearch {

	List<AddressBookRecord> search(Query query);

	List<AddressBookRecord> search(List<Predicate<? super AddressBookRecord>> predicates);

	Collection<AddressBookRecord> byNameOrEmailPrefix(String value);

	SearchResults byKind(List<String> kinds, int offset, int limit);

}
