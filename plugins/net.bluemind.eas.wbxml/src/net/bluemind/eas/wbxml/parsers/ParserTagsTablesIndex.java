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
package net.bluemind.eas.wbxml.parsers;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import net.bluemind.eas.wbxml.TagsTables;

public final class ParserTagsTablesIndex {

	private static final Map<Integer, NamespacedTable> index;

	static {
		HashMap<Integer, NamespacedTable> table = new HashMap<>();

		table.put(0, new NamespacedTable("AirSync", TagsTables.CP_0)); // AirSync
		table.put(1, new NamespacedTable("Contacts", TagsTables.CP_1)); // Contacts
		table.put(2, new NamespacedTable("Email", TagsTables.CP_2)); // Email
		table.put(3, new NamespacedTable("AirNotify", TagsTables.CP_3)); // AirNotify
		table.put(4, new NamespacedTable("Calendar", TagsTables.CP_4)); // Calendar
		table.put(5, new NamespacedTable("Move", TagsTables.CP_5)); // Move
		table.put(6, new NamespacedTable("GetItemEstimate", TagsTables.CP_6)); // GetItemEstimate
		table.put(7, new NamespacedTable("FolderHierarchy", TagsTables.CP_7)); // FolderHierarchy
		table.put(8, new NamespacedTable("MeetingResponse", TagsTables.CP_8)); // MeetingResponse
		table.put(9, new NamespacedTable("Tasks", TagsTables.CP_9)); // Tasks
		table.put(10, new NamespacedTable("ResolveRecipients", TagsTables.CP_10)); // ResolveRecipients
		table.put(11, new NamespacedTable("ValidateCert", TagsTables.CP_11)); // ValidateCert
		table.put(12, new NamespacedTable("Contacts2", TagsTables.CP_12)); // Contacts2
		table.put(13, new NamespacedTable("Ping", TagsTables.CP_13)); // Ping
		table.put(14, new NamespacedTable("Provision", TagsTables.CP_14)); // Provision
		table.put(15, new NamespacedTable("Search", TagsTables.CP_15)); // Search
		table.put(16, new NamespacedTable("GAL", TagsTables.CP_16)); // GAL
		table.put(17, new NamespacedTable("AirSyncBase", TagsTables.CP_17)); // AirSyncBase
		table.put(18, new NamespacedTable("Settings", TagsTables.CP_18)); // Settings
		table.put(19, new NamespacedTable("DocumentLibrary", TagsTables.CP_19)); // DocumentLibrary
		table.put(20, new NamespacedTable("ItemOperations", TagsTables.CP_20)); // ItemOperations
		table.put(21, new NamespacedTable("ComposeMail", TagsTables.CP_21)); // ComposeMail
		table.put(22, new NamespacedTable("Email2", TagsTables.CP_22)); // Email2
		table.put(23, new NamespacedTable("Notes", TagsTables.CP_23)); // Notes
		table.put(24, new NamespacedTable("RightsManagement", TagsTables.CP_24)); // RightsManagement

		index = ImmutableMap.copyOf(table);
	}

	public static Map<Integer, NamespacedTable> get() {
		return index;
	}

}
