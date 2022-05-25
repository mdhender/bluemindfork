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
package net.bluemind.core.container.hierarchy.hook;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class HierarchyIdsHints {
	private static final Cache<String, Long> idHints = CacheBuilder.newBuilder().recordStats().maximumSize(256)
			.expireAfterAccess(5, TimeUnit.MINUTES).build();

	public static void putHint(String uid, long id) {
		idHints.put(uid, id);
	}

	public static Long getHint(String uid) {
		Long ret = idHints.getIfPresent(uid);
		if (ret != null) {
			idHints.invalidate(uid);
		}
		return ret;
	}

	public static void invalidateAll() {
		idHints.invalidateAll();
	}

}
