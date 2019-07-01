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
package net.bluemind.ui.im.client.push.message;

public class MucMessage extends ServerNotification {
	protected MucMessage() {

	}

	public final native void debug() /*-{
		console.log(this);
	}-*/;

	public final native String getAction() /*-{
		return this.action;
	}-*/;

	private final native String _getFrom() /*-{
		return this.body.from;
	}-*/;

	public final native String getMessage() /*-{
		return this.body.message;
	}-*/;

	public final String getThreadId() {
		// room@muc.bm.lan/user
		String from = _getFrom();
		int idx = from.indexOf("/");
		return from.substring(0, idx);
	}

	public final String getFrom() {
		// room@muc.bm.lan/user
		String from = _getFrom();
		int idx = from.indexOf("/");
		return from.substring(idx + 1, from.length());
	}

	public final native void console(String s) /*-{
		console.log(s);
	}-*/;

	public final native String getRoom() /*-{
		return this.room;
	}-*/;

	private final native String _getParticipant() /*-{
		return this.participant;
	}-*/;

	public final String getParticipant() {
		String participant = _getParticipant();
		int idx = participant.indexOf("/");
		return participant.substring(idx + 1, participant.length());
	}

	public final native String getInvitationRoom() /*-{
		return this.body.room;
	}-*/;

	public final native String getInvitationReason() /*-{
		return this.body.reason;
	}-*/;

	private final native String _getInvitationInviter() /*-{
		return this.body.inviter;
	}-*/;

	public final String getInvitationInviter() {
		// user@bm.lan/resource
		String from = _getInvitationInviter();
		int idx = from.indexOf("/");
		return from.substring(0, idx);
	}

}
