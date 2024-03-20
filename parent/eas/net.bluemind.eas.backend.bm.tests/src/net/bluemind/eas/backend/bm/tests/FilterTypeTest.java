/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.eas.backend.bm.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.bm.ContentsExporter;
import net.bluemind.eas.dto.device.DeviceId;
import net.bluemind.eas.dto.sync.CollectionId;
import net.bluemind.eas.dto.sync.FilterType;
import net.bluemind.eas.dto.sync.SyncState;

public class FilterTypeTest {

	@Test
	public void testFilterChanges() {

		ContentsExporter exporter = new ContentsExporter(null, null, null, null);

		DeviceId device = new DeviceId("latd", "identifier", "devtype", "internalId");
		BackendSession bs = new BackendSession(null, device, 0);
		SyncState state = new SyncState();
		CollectionId collectionId = CollectionId.of("666");

		FilterType filterType = null; // initial
		boolean hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);
		// someSyncWithoutChangingAnything
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);

		// change type
		filterType = FilterType.THREE_DAYS_BACK; // initial
		// SPECIFIC filter never provokes change
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);
		// someSyncWithoutChangingAnything
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);
		// another change
		filterType = FilterType.ONE_DAY_BACK; // initial
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);

		// back to ALL, provokes change
		filterType = null;
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertTrue(hasChanged);
		// further calls using ALL do not provoke change
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);
		hasChanged = exporter.processFilterType(bs, state, filterType, collectionId);
		assertFalse(hasChanged);

	}

}
