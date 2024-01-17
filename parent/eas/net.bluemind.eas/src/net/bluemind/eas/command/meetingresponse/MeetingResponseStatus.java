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
package net.bluemind.eas.command.meetingresponse;

public enum MeetingResponseStatus {

	/**
	 * Success
	 */
	SUCCESS("1"),

	/**
	 * The client has sent a malformed or invalid item
	 */
	INVALID_MEETING_REQUEST("2"),

	/**
	 * An error occurred on the server
	 */
	SERVER_ERROR("3");

	private final String xmlValue;

	private MeetingResponseStatus(String xmlValue) {
		this.xmlValue = xmlValue;
	}

	public String asXmlValue() {
		return xmlValue;
	}

}
