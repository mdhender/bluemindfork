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
package net.bluemind.imap;

public enum Flag {
	SEEN, DRAFT, DELETED, FLAGGED, ANSWERED, BMARCHIVED, FORWARDED;

	public String toString() {
		switch (this) {
		case SEEN:
			return "\\Seen";
		case DRAFT:
			return "\\Draft";
		case DELETED:
			return "\\Deleted";
		case FLAGGED:
			return "\\Flagged";
		case ANSWERED:
			return "\\Answered";
		case FORWARDED:
			return "$Forwarded";
		case BMARCHIVED:
			return "Bmarchived";
		default:
			return "";
		}
	}

	public static Flag from(String s) {
		switch (s.toLowerCase()) {
		case "\\seen":
			return SEEN;
		case "\\draft":
			return SEEN;
		case "\\deleted":
			return DELETED;
		case "\\flagged":
			return FLAGGED;
		case "\\answered":
			return ANSWERED;
		case "$forwarded":
			return FORWARDED;
		case "bmarchived":
			return BMARCHIVED;
		default:
			return null;
		}
	}
}
