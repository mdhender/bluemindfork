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
package net.bluemind.eas.dto.foldersync;

public enum FolderType {

	USER_FOLDER_GENERIC(1), // 1
	DEFAULT_INBOX_FOLDER(2), // 2
	DEFAULT_DRAFTS_FOLDERS(3), // 3
	DEFAULT_DELETED_ITEMS_FOLDERS(4), // 4
	DEFAULT_SENT_EMAIL_FOLDER(5), // 5
	DEFAULT_OUTBOX_FOLDER(6), // 6
	DEFAULT_TASKS_FOLDER(7), // 7
	DEFAULT_CALENDAR_FOLDER(8), // 8
	DEFAULT_CONTACTS_FOLDER(9), // 9
	DEFAULT_NOTES_FOLDER(10), // 10
	DEFAULT_JOURNAL_FOLDER(11), // 11
	USER_CREATED_EMAIL_FOLDER(12), // 12
	USER_CREATED_CALENDAR_FOLDER(13), // 13
	USER_CREATED_CONTACTS_FOLDER(14), // 14
	USER_CREATED_TASKS_FOLDER(15), // 15
	USER_CREATED_JOURNAL_FOLDER(16), // 16
	USER_CREATED_NOTES_FOLDER(17), // 17
	UNKNOWN_FOLDER_TYPE(18); // 18

	private final int value;

	private FolderType(int value) {
		this.value = value;
	}

	public int asInt() {
		return value;
	}

	public static FolderType getValue(int type) {
		switch (type) {
		case 1:
			return USER_FOLDER_GENERIC;
		case 2:
			return DEFAULT_INBOX_FOLDER;
		case 3:
			return DEFAULT_DRAFTS_FOLDERS;
		case 4:
			return DEFAULT_DELETED_ITEMS_FOLDERS;
		case 5:
			return DEFAULT_SENT_EMAIL_FOLDER;
		case 6:
			return DEFAULT_OUTBOX_FOLDER;
		case 7:
			return DEFAULT_TASKS_FOLDER;
		case 8:
			return DEFAULT_CALENDAR_FOLDER;
		case 9:
			return DEFAULT_CONTACTS_FOLDER;
		case 10:
			return DEFAULT_NOTES_FOLDER;
		case 11:
			return DEFAULT_JOURNAL_FOLDER;
		case 12:
			return USER_CREATED_EMAIL_FOLDER;
		case 13:
			return USER_CREATED_CALENDAR_FOLDER;
		case 14:
			return USER_CREATED_CONTACTS_FOLDER;
		case 15:
			return USER_CREATED_TASKS_FOLDER;
		case 16:
			return USER_CREATED_JOURNAL_FOLDER;
		case 17:
			return USER_CREATED_NOTES_FOLDER;
		case 18:
		default:
			return UNKNOWN_FOLDER_TYPE;
		}
	}

}
