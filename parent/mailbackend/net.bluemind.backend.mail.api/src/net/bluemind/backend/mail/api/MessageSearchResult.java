/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General public final License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.mail.api;

import java.util.Date;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MessageSearchResult {

	public String containerUid;
	public int itemId;
	public String subject;
	public int size;
	public String messageClass;
	public Date date;
	public Mbox from;
	public Mbox to;
	public boolean seen;
	public boolean flagged;
	public boolean hasAttachment;
	public String preview;

	public MessageSearchResult() {

	}

	public MessageSearchResult(String containerUid, int itemId, String subject, int size, String messageClass,
			Date date, Mbox from, Mbox to, boolean seen, boolean flagged, boolean hasAttachment, String preview) {
		this.containerUid = containerUid;
		this.itemId = itemId;
		this.subject = subject;
		this.size = size;
		this.messageClass = messageClass;
		this.date = date;
		this.from = from;
		this.to = to;
		this.seen = seen;
		this.flagged = flagged;
		this.hasAttachment = hasAttachment;
		this.preview = preview;
	}

	@BMApi(version = "3")
	public final static class Mbox {
		public String displayName;
		public String address;
		public String routingType;

		public Mbox() {

		}

		public Mbox(String displayName, String address, String routingType) {
			this.displayName = displayName;
			this.address = address;
			this.routingType = routingType;
		}

		public final static Mbox create(String displayName, String address) {
			return new Mbox(displayName, address, "EX");

		}

		public final static Mbox create(String displayName, String address, String routingType) {
			return new Mbox(displayName, address, routingType);

		}
	};
}
