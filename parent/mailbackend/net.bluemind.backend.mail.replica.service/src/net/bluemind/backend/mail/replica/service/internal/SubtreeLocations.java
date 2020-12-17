/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.replica.service.internal;

import java.sql.SQLException;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import net.bluemind.backend.mail.replica.persistence.ReplicasStore;
import net.bluemind.backend.mail.replica.persistence.ReplicasStore.SubtreeLocation;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.caches.registry.CacheRegistry;
import net.bluemind.core.caches.registry.ICacheRegistration;

public class SubtreeLocations {
	static final Cache<String, SubtreeLocation> locations = Caffeine.newBuilder().recordStats().maximumSize(1024)
			.build();

	public static class CacheRegistration implements ICacheRegistration {
		@Override
		public void registerCaches(CacheRegistry cr) {
			cr.register(SubtreeLocation.class, locations);
		}
	}

	static Optional<SubtreeLocation> getById(ReplicasStore store, String mailboxUniqueId) {
		SubtreeLocation location = SubtreeLocations.locations.getIfPresent(mailboxUniqueId);
		if (location == null) {
			try {
				location = store.byUniqueId(mailboxUniqueId);
				if (location != null) {
					SubtreeLocations.locations.put(mailboxUniqueId, location);
				}
			} catch (SQLException e1) {
				throw ServerFault.sqlFault(e1);
			}
		}
		return Optional.ofNullable(location);
	}

}
