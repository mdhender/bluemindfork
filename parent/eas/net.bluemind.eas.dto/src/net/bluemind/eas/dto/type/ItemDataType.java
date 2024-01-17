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
package net.bluemind.eas.dto.type;

public enum ItemDataType {

	EMAIL, CALENDAR, CONTACTS, TASKS, NOTES, FOLDER;

	public String asXmlValue() {
		switch (this) {
		case CALENDAR:
			return "CALENDAR";
		case CONTACTS:
			return "CONTACTS";
		case TASKS:
			return "TASKS";
		case NOTES:
			return "NOTES";
		case FOLDER:
			return "FOLDER";
		default:
		case EMAIL:
			return "EMAIL";
		}
	}

	public static ItemDataType fromIntValue(int value) {
		switch (value) {
		case 0:
			return EMAIL;
		case 1:
			return CALENDAR;
		case 2:
			return CONTACTS;
		case 3:
			return TASKS;
		case 4:
		default:
			return FOLDER;
		}
	}

	public static ItemDataType getValue(String containerType) {
		switch (containerType) {
		case "mailbox_records":
			return EMAIL;
		case "calendar":
			return CALENDAR;
		case "addressbook":
			return CONTACTS;
		case "todolist":
			return TASKS;
		default:
			return FOLDER;
		}
	}

}
