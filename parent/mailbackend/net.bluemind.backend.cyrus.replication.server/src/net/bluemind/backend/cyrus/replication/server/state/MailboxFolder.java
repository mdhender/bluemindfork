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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import net.bluemind.backend.cyrus.replication.protocol.parsing.JsUtils;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.mail.replica.api.MailboxAnnotation;

public class MailboxFolder {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(MailboxFolder.class);

	private String name;
	private String uniqueId;
	private long lastUid;
	private long highestModSeq;
	private long recentUid;
	private long recentTime;
	private long lastAppendDate;
	private long pop3lastLogin;
	private long uidValidity;
	private String partition;
	private String acl;
	private String options;
	private long syncCRC;
	private String quotaRoot;
	private List<MailboxAnnotation> annotations = Collections.emptyList();

	public static MailboxFolder of(JsonObject mbox) {
		MailboxFolder mf = new MailboxFolder();
		mf.name = Token.atomOrValue(mbox.getString("MBOXNAME"));
		mf.uniqueId = mbox.getString("UNIQUEID");
		mf.lastUid = Long.parseLong(mbox.getString("LAST_UID"));
		mf.highestModSeq = Long.parseLong(mbox.getString("HIGHESTMODSEQ"));
		mf.recentUid = Long.parseLong(mbox.getString("RECENTUID"));
		mf.recentTime = Long.parseLong(mbox.getString("RECENTTIME"));
		mf.lastAppendDate = Long.parseLong(mbox.getString("LAST_APPENDDATE"));
		mf.pop3lastLogin = Long.parseLong(mbox.getString("POP3_LAST_LOGIN"));
		mf.uidValidity = Long.parseLong(mbox.getString("UIDVALIDITY"));
		mf.partition = mbox.getString("PARTITION");
		mf.acl = mbox.getString("ACL");
		mf.options = mbox.getString("OPTIONS");
		// brand new in cyrus 3
		if (mbox.containsField("ANNOTATIONS")) {
			mf.annotations = JsUtils.asList(mbox.getArray("ANNOTATIONS"), (JsonObject obj) -> {
				MailboxAnnotation ma = MailboxAnnotation.of(obj);
				ma.mailbox = mf.name;
				ma.value = Token.atomOrValue(ma.value);
				return ma;
			});
			logger.debug("ANNOTS: {}", mf.annotations);
		}
		mf.syncCRC = Long.parseLong(mbox.getString("SYNC_CRC"));
		mf.quotaRoot = mbox.getString("QUOTAROOT");
		return mf;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public long getLastUid() {
		return lastUid;
	}

	public void setLastUid(long lastUid) {
		this.lastUid = lastUid;
	}

	public long getHighestModSeq() {
		return highestModSeq;
	}

	public void setHighestModSeq(long highestModSeq) {
		this.highestModSeq = highestModSeq;
	}

	public long getRecentUid() {
		return recentUid;
	}

	public void setRecentUid(long recentUid) {
		this.recentUid = recentUid;
	}

	public long getRecentTime() {
		return recentTime;
	}

	public void setRecentTime(long recentTime) {
		this.recentTime = recentTime;
	}

	public long getLastAppendDate() {
		return lastAppendDate;
	}

	public void setLastAppendDate(long lastAppendDate) {
		this.lastAppendDate = lastAppendDate;
	}

	public long getPop3lastLogin() {
		return pop3lastLogin;
	}

	public void setPop3lastLogin(long pop3lastLogin) {
		this.pop3lastLogin = pop3lastLogin;
	}

	public long getUidValidity() {
		return uidValidity;
	}

	public void setUidValidity(long uidValidity) {
		this.uidValidity = uidValidity;
	}

	public String getPartition() {
		return partition;
	}

	public void setPartition(String partition) {
		this.partition = partition;
	}

	public String getAcl() {
		return acl;
	}

	public void setAcl(String acl) {
		this.acl = acl;
	}

	public String getOptions() {
		return options;
	}

	public void setOptions(String options) {
		this.options = options;
	}

	public long getSyncCRC() {
		return syncCRC;
	}

	public void setSyncCRC(long syncCRC) {
		this.syncCRC = syncCRC;
	}

	/**
	 * %(UNIQUEID 5596488a58661ddc MBOXNAME vagrant.vmw!user.admin LAST_UID 1
	 * HIGHESTMODSEQ 10 RECENTUID 1 RECENTTIME 1483104873 LAST_APPENDDATE 1483088316
	 * POP3_LAST_LOGIN 0 UIDVALIDITY 1483087324 PARTITION vagrant_vmw ACL
	 * "admin@vagrant.vmw lrswipkxtecda admin0 lrswipkxtecda " OPTIONS P SYNC_CRC
	 * 3758469704)
	 * 
	 * @return paren-object representation
	 */
	public String toParenObjectString() {
		StringBuilder sb = new StringBuilder();
		sb.append("%(");
		mailboxFields(sb);
		sb.append(")");
		return sb.toString();
	}

	public String toParenObjectString(List<MboxRecord> withRecords) {
		StringBuilder sb = new StringBuilder();
		sb.append("%(");
		mailboxFields(sb);
		sb.append(" RECORD (");
		for (MboxRecord mr : withRecords) {
			sb.append(" ").append(mr.toParentObjectString());
		}
		sb.append(")");
		sb.append(")");
		return sb.toString();
	}

	private void mailboxFields(StringBuilder sb) {
		sb.append("UNIQUEID ").append(uniqueId);
		sb.append(" MBOXNAME ").append(quoteIfNeeded(name));
		sb.append(" SYNC_CRC ").append(syncCRC);
		// FIXME cyrus 3 new
		sb.append(" SYNC_CRC_ANNOT ").append("0");
		sb.append(" LAST_UID ").append(lastUid);
		sb.append(" HIGHESTMODSEQ ").append(highestModSeq);
		sb.append(" RECENTUID ").append(recentUid);
		sb.append(" RECENTTIME ").append(recentTime);
		sb.append(" LAST_APPENDDATE ").append(lastAppendDate);
		sb.append(" POP3_LAST_LOGIN ").append(pop3lastLogin);
		// FIXME cyrus 3 new
		sb.append(" POP3_SHOW_AFTER ").append("0");
		sb.append(" UIDVALIDITY ").append(uidValidity);
		sb.append(" PARTITION ").append(partition);
		sb.append(" ACL \"").append(acl).append("\"");
		sb.append(" OPTIONS ").append(options);
		if (!annotations.isEmpty()) {
			System.err.println("should write annotations");
		}
		if (quotaRoot != null) {
			sb.append(" QUOTAROOT ").append(quotaRoot);
		}
	}

	public String getQuotaRoot() {
		return quotaRoot;
	}

	private String quoteIfNeeded(String s) {
		if (s.contains(" ")) {
			return "\"" + s + "\"";
		} else {
			return s;
		}
	}

	public void setQuotaRoot(String quotaRoot) {
		this.quotaRoot = quotaRoot;
	}

	public List<MailboxAnnotation> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(List<MailboxAnnotation> annotations) {
		this.annotations = annotations;
	}

}
