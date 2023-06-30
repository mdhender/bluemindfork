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
package net.bluemind.eas.dto;

public enum NamespaceMapping {

	AIR_SYNC_BASE(null, "AirSyncBase"), //
	SYNC("Sync", "AirSync"), //
	EMAIL(null, "Email"), //
	EMAIL_2(null, "Email2"), //
	CALENDAR(null, "Calendar"), //
	CONTACTS(null, "Contacts"), //
	CONTACTS_2(null, "Contacts2"), //
	GAL(null, "GAL"), //
	FIND("Find", "Find"), //
	FOLDER_SYNC("FolderSync", "FolderHierarchy"), //
	FOLDER_CREATE("FolderCreate", "FolderHierarchy"), //
	FOLDER_DELETE("FolderDelete", "FolderHierarchy"), //
	FOLDER_UPDATE("FolderUpdate", "FolderHierarchy"), //
	SETTINGS("Settings", "Settings"), //
	SEND_MAIL("SendMail", "ComposeMail"), //
	SMART_REPLY("SmartReply", "ComposeMail"), //
	SMART_FORWARD("SmartForward", "ComposeMail"), //
	GET_ATTACHMENT("GetAttachment", "GetAttachment"), //
	GET_ITEM_ESTIMATE("GetItemEstimate", "GetItemEstimate"), //
	PROVISION("Provision", "Provision"), //
	MEETING_RESPONSE("MeetingResponse", "MeetingResponse"), //
	MOVE_ITEMS("MoveItems", "Move"), //
	PING("Ping", "Ping"), //
	ITEM_OPERATIONS("ItemOperations", "ItemOperations"), //
	RESOLVE_RECIPIENTS("ResolveRecipients", "ResolveRecipients"), //
	SEARCH("Search", "Search"), //
	TASKS("Tasks", "Tasks"), //
	OPTIONS("Options", "AirSync");

	private final String root;
	private final String ns;

	private NamespaceMapping(String root, String ns) {
		this.root = root;
		this.ns = ns;
	}

	public String namespace() {
		return ns;
	}

	public String root() {
		return root;
	}

	public static NamespaceMapping of(String root) {
		for (NamespaceMapping val : NamespaceMapping.values()) {
			if (root.equals(val.root)) {
				return val;
			}
		}
		return null;
	}

}
