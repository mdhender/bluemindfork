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
package net.bluemind.eas.dto.email;

public enum MessageClass {

	/**
	 * Normal e-mail message
	 */
	NOTE("IPM.Note", "IPM.Note"),

	/**
	 * Secure MIME (S/MIME) encrypted and opaque-signed message
	 */
	NOTE_SMIME("IPM.Note.SMIME", "IPM.Note.SMIME"),

	/**
	 * S/MIME clear-signed message
	 */
	NOTE_SMIME_MULTIPART_SIGNED("IPM.Note.SMIME.MultipartSigned", "IPM.Note.SMIME.MultipartSigned"),

	/**
	 * message is a secure read receipt
	 */
	NOTE_RECEIPT_SMIME(null, "IPM.Note.Receipt.SMIME"),

	/**
	 * An InfoPath form
	 */
	INFO_PATH_FORM(null, "IPM.InfoPathForm"),

	/**
	 * Message containing a meeting request
	 */
	SCHEDULE_MEETING_REQUEST("IPM.Schedule.Meeting.Request", "IPM.Schedule.Meeting.Request"),

	/**
	 * Meeting notification
	 */
	NOTIFICATION_MEETING(null, "IPM.Notification.Meeting"),

	/**
	 * Octel voice message
	 */
	OCTEL_VOICE(null, "IPM.Octel.Voice"),

	/**
	 * Electronic voice notes
	 */
	VOICE_NOTES(null, "IPM.Voicenotes"),

	/**
	 * Shared message
	 */
	SHARING(null, "IPM.Sharing"),

	/**
	 * Notification of a canceled meeting
	 */
	SCHEDULE_MEETING_CANCELED("IPM.Schedule.Meeting.Canceled", "IPM.Schedule.Meeting.Canceled"),

	/**
	 * Accepted meeting request
	 */
	SCHEDULE_MEETING_RESP_POS("IPM.Schedule.Meeting.Resp.Pos", null),

	/**
	 * Tentatively accepted meeting request
	 */
	SCHEDULE_MEETING_RESP_TENT("IPM.Schedule.Meeting.Resp.Tent", null),

	/**
	 * Declined meeting request
	 */
	SCHEDULE_MEETING_RESP_NEG("IPM.Schedule.Meeting.Resp.Neg", null),

	/**
	 * Post
	 */
	POST("IPM.Post", "IPM.Post");

	private String proto12Value;
	private String proto14Value;

	private MessageClass(String p12, String p14) {
		this.proto12Value = p12;
		this.proto14Value = p14;
	}

	public String toString(double protocolVersion) {
		String ret;
		if (protocolVersion < 14) {
			ret = proto12Value;
		} else {
			ret = proto14Value;
		}
		if (ret == null) {
			ret = "IPM.Note";
		}
		return ret;
	}

}
