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
package net.bluemind.eas.client;

/**
 * Possible values for the status element in MoveItems reponses
 * 
 * 
 */
public enum MoveStatus {

	INVALID_SOURCE_COLLECTION_ID, // 1
	INVALID_DESTINATION_COLLECTION_ID, // 2
	SUCCESS, // 3
	SAME_SOURCE_AND_DESTINATION_COLLECTION_ID, // 4
	SERVER_ERROR, // 5
	ITEM_ALREADY_EXISTS_AT_DESTINATION, // 6
	SOURCE_OR_DESTINATION_LOCKED; // 7

	public static MoveStatus getSyncStatus(int type) {
		switch (type) {
		case 1:
			return INVALID_SOURCE_COLLECTION_ID;
		case 2:
			return INVALID_DESTINATION_COLLECTION_ID;
		case 3:
			return SUCCESS;
		case 4:
			return SAME_SOURCE_AND_DESTINATION_COLLECTION_ID;
		case 5:
			return SERVER_ERROR;
		case 6:
			return ITEM_ALREADY_EXISTS_AT_DESTINATION;
		case 7:
			return SOURCE_OR_DESTINATION_LOCKED;
		default:
			return null;
		}
	}

	public String asXmlValue() {
		switch (this) {
		case SOURCE_OR_DESTINATION_LOCKED:
			return "7";
		case ITEM_ALREADY_EXISTS_AT_DESTINATION:
			return "6";
		case INVALID_SOURCE_COLLECTION_ID:
			return "1";
		case INVALID_DESTINATION_COLLECTION_ID:
			return "2";
		case SAME_SOURCE_AND_DESTINATION_COLLECTION_ID:
			return "4";
		case SERVER_ERROR:
			return "5";
		case SUCCESS:
		default:
			return "3";
		}
	}
}
