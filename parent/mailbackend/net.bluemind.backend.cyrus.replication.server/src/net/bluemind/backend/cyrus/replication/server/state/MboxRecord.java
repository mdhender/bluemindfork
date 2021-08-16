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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.backend.cyrus.replication.server.state;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import net.bluemind.backend.mail.replica.api.MailboxRecordAnnotation;

public class MboxRecord {

	// mutable part...
	private MailboxFolder parent;
	private List<String> flags;
	private long uid;
	private long size;
	private long modseq;
	private long lastUpdated;
	private long internalDate;
	private String bodyGuid;
	private List<MailboxRecordAnnotation> annotations = Collections.emptyList();

	public static class MessageRecordBuilder {

		private List<String> flags;
		private long uid;
		private long size;
		private long modseq;
		private long lastUpdated;
		private long internalDate;
		private String bodyGuid;
		private List<MailboxRecordAnnotation> annotations = Collections.emptyList();

		private MessageRecordBuilder() {
			this.flags = Collections.emptyList();
		}

		public MessageRecordBuilder body(String bodyGuid) {
			this.bodyGuid = bodyGuid;
			return this;
		}

		public MessageRecordBuilder uid(long uid) {
			this.uid = uid;
			return this;
		}

		public MessageRecordBuilder modseq(long modseq) {
			this.modseq = modseq;
			return this;
		}

		public MessageRecordBuilder lastUpdated(long date) {
			this.lastUpdated = date;
			return this;
		}

		public MessageRecordBuilder internalDate(long date) {
			this.internalDate = date;
			return this;
		}

		public MessageRecordBuilder size(long size) {
			this.size = size;
			return this;
		}

		public MessageRecordBuilder flags(Iterable<String> flags) {
			this.flags = Lists.newArrayList(flags);
			return this;
		}

		public MessageRecordBuilder annotations(List<MailboxRecordAnnotation> annotations) {
			this.annotations = annotations;
			return this;
		}

		public MboxRecord build() {
			return new MboxRecord(bodyGuid, uid, modseq, lastUpdated, internalDate, flags, size, annotations);
		}

	}

	public static MessageRecordBuilder builder() {
		return new MessageRecordBuilder();
	}

	private MboxRecord(String bodyGuid, long uid, long modseq, long lastUpdated, long internalDate, List<String> flags,
			long size, List<MailboxRecordAnnotation> annotations) {
		this.bodyGuid = bodyGuid;
		this.uid = uid;
		this.modseq = modseq;
		this.lastUpdated = lastUpdated;
		this.internalDate = internalDate;
		this.flags = flags;
		this.size = size;
		this.annotations = annotations;
	}

	public String bodyGuid() {
		return bodyGuid;
	}

	public MailboxFolder parent() {
		return parent;
	}

	public void attachToParent(MailboxFolder mf) {
		this.parent = mf;
	}

	public long uid() {
		return uid;
	}

	public long modseq() {
		return modseq;
	}

	public long internalDate() {
		return internalDate;
	}

	public long lastUpdated() {
		return lastUpdated;
	}

	public List<String> flags() {
		return flags;
	}

	public long size() {
		return size;
	}

	public List<MailboxRecordAnnotation> annotations() {
		return annotations;
	}

	public String toParentObjectString() {
		StringBuilder sb = new StringBuilder();
		sb.append("%(");
		sb.append("UID ").append(uid());
		sb.append(" MODSEQ ").append(modseq());
		sb.append(" LAST_UPDATED ").append(lastUpdated());
		sb.append(" FLAGS (").append(String.join(" ", flags())).append(")");
		sb.append(" INTERNALDATE ").append(internalDate);
		if (size > 0) {
			sb.append(" SIZE ").append(size);
		} else {
			sb.append(" SIZE ").append(666);
		}
		sb.append(" GUID ").append(bodyGuid());
		if (!annotations.isEmpty()) {
			sb.append(" ANNOTATIONS (");
			boolean first = true;
			for (MailboxRecordAnnotation mra : annotations) {
				sb.append(first ? "" : " ").append(mra.toParenObjectString());
				first = false;
			}
			sb.append(")");
		}
		sb.append(")");
		return sb.toString();
	}

}
