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

import java.io.IOException;
import java.io.OutputStream;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.bluemind.backend.mail.replica.api.MailboxReplica;

public class CyrusIndexWriter {
	public static final int DEFAULT_VERSION = 13;

	protected OutputStream out;
	protected final int version;

	public CyrusIndexWriter(OutputStream out) {
		this(out, DEFAULT_VERSION);
	}

	public CyrusIndexWriter(OutputStream out, int version) {
		this.out = out;
		this.version = version;
	}

	private void writeHeader(MailboxReplica mf, int recordscount) throws IOException {
		ByteBuf buf = Unpooled.buffer(CyrusIndexHeader.getHeaderSize(version));
		CyrusIndexHeader hdr = new CyrusIndexHeader(version);
		hdr.generation = 1; // Don't ask me what it is
		hdr.format = 0; // don't ask either please !
		hdr.startOffset = CyrusIndexHeader.getHeaderSize(version);
		hdr.numRecords = recordscount;
		hdr.lastAppendDate = mf.lastAppendDate.getTime();
		hdr.lastUid = (int) mf.lastUid;
		hdr.quotaUsed = 0;
		hdr.pop3LastLogin = (int) mf.pop3LastLogin.getTime();
		hdr.uidValidity = (int) mf.uidValidity;
		hdr.deleted = 0;
		hdr.answered = 0;
		hdr.flagged = 0;
		hdr.leakedCache = 0;
		hdr.highestModseq = mf.highestModSeq;
		hdr.deletedModseq = 0;
		hdr.exists = recordscount;
		hdr.firstExpunged = 0L;
		hdr.lastCleanup = 0L;
		hdr.headerFileCRC = 0;
		hdr.syncCRCsBasic = 0;
		hdr.recentUid = (int) mf.recentUid;
		hdr.recentTime = mf.recentTime.getTime();
		hdr.pop3ShowAfter = 0;
		hdr.quotaAnnotUsed = 0;
		hdr.syncCRCsAnnot = 0;
		hdr.unseen = 0;
		hdr.headerCrc = 0;
		hdr.to(buf);
		buf.readBytes(out, buf.readableBytes());
	}

	public void writeAllRecords(MailboxReplica folder, Iterable<CyrusIndexMailboxRecord> records, int recordsSize)
			throws IOException {
		writeHeader(folder, recordsSize);
		ByteBuf buf = Unpooled.buffer(CyrusIndexHeader.getRecordSize(version));
		for (CyrusIndexMailboxRecord rec : records) {
			from(version, rec).to(buf);
			buf.readBytes(out, buf.readableBytes());
			buf.clear();
		}
	}

	public static CyrusIndexRecord from(int version, CyrusIndexMailboxRecord mrec) {
		CyrusIndexRecord irec = new CyrusIndexRecord(version);
		irec.uid = (int) mrec.imapUid;
		irec.internalDate = mrec.internalDateInt();
		irec.sentDate = mrec.lastUpdatedInt();
		irec.size = mrec.size();
		irec.headerSize = 0; // can cyrus recalculate this ?
		irec.contentLines = 0; // can cyrus recalculate this ?
		irec.gmTime = 0;
		irec.lastUpdated = mrec.lastUpdatedInt();
		irec.saveDate = mrec.sentDateInt();
		irec.guid = mrec.guid();
		irec.modseq = mrec.modSeq();
		irec.setSystemFlags(mrec.systemFlags(), mrec.otherFlags());
		// Don't really know how to extract userFlags
		// irec.setUserFlags();

		// We don't handle the caching, let cyrus recalculate it with reconstruct
		irec.cacheVersion = 0;
		irec.cacheOffset = 0;
		irec.cacheCRC = 0;
		// Conversation ID
		irec.cid = ByteBufUtil.hexDump(new byte[8]); // Conversation ID
		// recordCRC will be calculated by CyrusIndexRecord.to(ByteBuf)
		return irec;
	}
}
