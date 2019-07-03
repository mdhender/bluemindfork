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

	AirSyncBase(null, "AirSyncBase"), //
	Sync("Sync", "AirSync"), //
	Email(null, "Email"), //
	Email2(null, "Email2"), //
	Calendar(null, "Calendar"), //
	Contacts(null, "Contacts"), //
	Contacts2(null, "Contacts2"), //
	GAL(null, "GAL"), //
	FolderSync("FolderSync", "FolderHierarchy"), //
	FolderCreate("FolderCreate", "FolderHierarchy"), //
	FolderDelete("FolderDelete", "FolderHierarchy"), //
	FolderUpdate("FolderUpdate", "FolderHierarchy"), //
	Settings("Settings", "Settings"), //
	SendMail("SendMail", "ComposeMail"), //
	SmartReply("SmartReply", "ComposeMail"), //
	SmartForward("SmartForward", "ComposeMail"), //
	GetAttachment("GetAttachment", "GetAttachment"), //
	GetItemEstimate("GetItemEstimate", "GetItemEstimate"), //
	Provision("Provision", "Provision"), //
	MeetingResponse("MeetingResponse", "MeetingResponse"), //
	MoveItems("MoveItems", "Move"), //
	Ping("Ping", "Ping"), //
	ItemOperations("ItemOperations", "ItemOperations"), //
	ResolveRecipients("ResolveRecipients", "ResolveRecipients"), //
	Search("Search", "Search"), //
	Tasks("Tasks", "Tasks");

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
}
