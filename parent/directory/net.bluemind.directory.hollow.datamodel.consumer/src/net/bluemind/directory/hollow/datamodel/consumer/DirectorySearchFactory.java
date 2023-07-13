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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DirectorySearchFactory {

	private static Map<String, DirectoryDeserializer> deserializers = new ConcurrentHashMap<>();

	private DirectorySearchFactory() {

	}

	public static SerializedDirectorySearch get(String domain) {
		return new DefaultDirectorySearch(deserializer(domain));
	}

	/**
	 * The instance returned here is not cached & is limited to the last snapshot.
	 * Updates will not be tracked.
	 * 
	 * @param domain
	 * @return
	 */
	public static SerializedDirectorySearch createSnapshot(String domain) {
		return new DefaultDirectorySearch(new DirectoryDeserializer(domain, false));
	}

	public static BrowsableDirectorySearch browser(String domain) {
		return new FilteredDirectorySearch(deserializer(domain), r -> !r.getHidden());
	}

	private static DirectoryDeserializer deserializer(String domain) {
		return deserializers.computeIfAbsent(domain, DirectoryDeserializer::new);
	}

	public static Map<String, DirectoryDeserializer> getDeserializers() {
		return DirectorySearchFactory.deserializers;
	}

	public static void reset() {
		DirectorySearchFactory.deserializers.values().forEach(deserializer -> deserializer.context.stopWatcher());
		DirectorySearchFactory.deserializers = new ConcurrentHashMap<>();
	}

}
