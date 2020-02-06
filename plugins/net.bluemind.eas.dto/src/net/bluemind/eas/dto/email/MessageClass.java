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
	Note("IPM.Note", "IPM.Note"),

	/**
	 * Secure MIME (S/MIME) encrypted and opaque-signed message
	 */
	NoteSMIME("IPM.Note.SMIME", "IPM.Note.SMIME"),

	/**
	 * S/MIME clear-signed message
	 */
	NoteSMIMEMultipartSigned("IPM.Note.SMIME.MultipartSigned", "IPM.Note.SMIME.MultipartSigned"),

	/**
	 * message is a secure read receipt
	 */
	NoteReceiptSMIME(null, "IPM.Note.Receipt.SMIME"),

	/**
	 * An InfoPath form
	 */
	InfoPathForm(null, "IPM.InfoPathForm"),

	/**
	 * Message containing a meeting request
	 */
	ScheduleMeetingRequest("IPM.Schedule.Meeting.Request", "IPM.Schedule.Meeting.Request"),

	/**
	 * Meeting notification
	 */
	NotificationMeeting(null, "IPM.Notification.Meeting"),

	/**
	 * Octel voice message
	 */
	OctelVoice(null, "IPM.Octel.Voice"),

	/**
	 * Electronic voice notes
	 */
	Voicenotes(null, "IPM.Voicenotes"),

	/**
	 * Shared message
	 */
	Sharing(null, "IPM.Sharing"),

	/**
	 * Notification of a canceled meeting
	 */
	ScheduleMeetingCanceled("IPM.Schedule.Meeting.Canceled", "IPM.Schedule.Meeting.Canceled"),

	/**
	 * Accepted meeting request
	 */
	ScheduleMeetingRespPos("IPM.Schedule.Meeting.Resp.Pos", null),

	/**
	 * Tentatively accepted meeting request
	 */
	ScheduleMeetingRespTent("IPM.Schedule.Meeting.Resp.Tent", null),

	/**
	 * Declined meeting request
	 */
	ScheduleMeetingRespNeg("IPM.Schedule.Meeting.Resp.Neg", null),

	/**
	 * Post
	 */
	Post("IPM.Post", "IPM.Post");

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
