/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.index;

import java.util.Date;

public class CyrusIndexMailboxRecord {
	protected long modSeq;
	protected Date internalDate;
	protected Date lastUpdated;
	protected Date sentDate;
	protected int systemFlags;
	protected String otherFlags;
	protected String guid;
	protected int imapUid;
	protected int size;

	public CyrusIndexMailboxRecord() {
		// ok
	}

	public CyrusIndexMailboxRecord modSeq(long modSeq) {
		this.modSeq = modSeq;
		return this;
	}

	public long modSeq() {
		return modSeq;
	}

	public CyrusIndexMailboxRecord internalDate(Date internalDate) {
		this.internalDate = internalDate;
		return this;
	}

	public Date internalDate() {
		return internalDate;
	}

	public int internalDateInt() {
		return this.internalDate != null ? (int) internalDate.getTime() / 1000 : 0;
	}

	public CyrusIndexMailboxRecord sentDate(Date sentDate) {
		this.sentDate = sentDate;
		return this;
	}

	public Date sentDate() {
		return sentDate;
	}

	public int sentDateInt() {
		return this.sentDate != null ? (int) sentDate.getTime() / 1000 : 0;
	}

	public CyrusIndexMailboxRecord lastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
		return this;
	}

	public Date lastUpdated() {
		return lastUpdated;
	}

	public int lastUpdatedInt() {
		return this.lastUpdated != null ? (int) lastUpdated.getTime() / 1000 : 0;
	}

	public CyrusIndexMailboxRecord guid(String guid) {
		this.guid = guid;
		return this;
	}

	public String guid() {
		return guid;
	}

	public CyrusIndexMailboxRecord otherFlags(String otherFlags) {
		this.otherFlags = otherFlags;
		return this;
	}

	public String otherFlags() {
		return otherFlags;
	}

	public CyrusIndexMailboxRecord imapUid(int imapUid) {
		this.imapUid = imapUid;
		return this;
	}

	public int imapUid() {
		return imapUid;
	}

	public CyrusIndexMailboxRecord systemFlags(int systemFlags) {
		this.systemFlags = systemFlags;
		return this;
	}

	public int systemFlags() {
		return systemFlags;
	}

	public CyrusIndexMailboxRecord size(int size) {
		this.size = size;
		return this;
	}

	public int size() {
		return size;
	}
}