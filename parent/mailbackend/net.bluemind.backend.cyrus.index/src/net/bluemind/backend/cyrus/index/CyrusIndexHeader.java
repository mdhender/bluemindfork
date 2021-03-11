/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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
import java.io.InputStream;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class CyrusIndexHeader {
	private static final Logger logger = LoggerFactory.getLogger(CyrusIndex.class);

	public final int headerSize;

	public int generation; // int32 4
	public int format; // int32 4
	public final int version; // int32 4
	public int startOffset; // int32 4
	public final int recordSize; // int32 4
	public int numRecords; // int32 4
	public long lastAppendDate; // time_t 4
	public int lastUid; // int32 4
	public int quotaUsed; // int64 8
	public long pop3LastLogin; // time_t 4
	public int uidValidity; // int32 4
	public int deleted; // int32 4
	public int answered; // int32 4
	public int flagged; // int32 4
	public final byte[] options = new byte[4]; // bitmap 4
	public int leakedCache; // int32 4
	public long highestModseq; // int64 8
	public long deletedModseq; // int64 8
	public int exists; // int32 4
	public long firstExpunged; // time_t 4
	public long lastCleanup; // time_t 4
	public int headerFileCRC; // int32 4
	public int syncCRCsBasic; // int32 4
	public int recentUid; // int32 4
	public long recentTime; // time_t 4
	public int pop3ShowAfter; // int32 4
	public int quotaAnnotUsed; // int32 4
	public int syncCRCsAnnot; // int32 4
	public int unseen; // int32 4
	public int headerCrc; // int32 4

	public CyrusIndexHeader(int version) {
		this.version = version;
		this.headerSize = getHeaderSize(version);
		this.recordSize = getRecordSize(version);
	}

	public String toString() {
		return String.format("<CyrusIndexHeader version=%d numRecords=%d>", version, numRecords);
	}

	public CyrusIndexHeader from(ByteBuf buffer) throws UnknownVersion {
		generation = buffer.getInt(0);
		format = buffer.getInt(4);
		startOffset = buffer.getInt(12);
		exists = buffer.getInt(20);
		lastAppendDate = buffer.getUnsignedInt(24);
		lastUid = buffer.getInt(28);
		quotaUsed = buffer.getInt(32);
		pop3LastLogin = buffer.getUnsignedInt(40);
		uidValidity = buffer.getInt(44);
		deleted = buffer.getInt(48);
		answered = buffer.getInt(52);
		flagged = buffer.getInt(56);
		buffer.getBytes(60, options);
		leakedCache = buffer.getInt(64);
		highestModseq = buffer.getLong(68);

		if (version < 12) {
			exists = buffer.getInt(20);
			numRecords = 0;
		}

		if (version >= 12) {
			numRecords = buffer.getInt(20);
			deletedModseq = buffer.getLong(76);
			exists = buffer.getInt(84);
			firstExpunged = buffer.getUnsignedInt(88);
			lastCleanup = buffer.getUnsignedInt(92);
			headerFileCRC = buffer.getInt(96);
			syncCRCsBasic = buffer.getInt(100);
			recentUid = buffer.getInt(104);
			recentTime = buffer.getUnsignedInt(108);
		}
		if (version >= 13) {
			pop3ShowAfter = buffer.getInt(112);
			quotaAnnotUsed = buffer.getInt(116);
			syncCRCsAnnot = buffer.getInt(120);
		}

		switch (version) {
		case 9:
		case 10:
		case 11:
			headerCrc = buffer.getInt(92);
			break;
		case 12:
		case 13:
			headerCrc = buffer.getInt(124);
			break;
		case 14:
		case 15:
			headerCrc = buffer.getInt(156);
			break;
		default:
			throw new UnknownVersion("unknown version " + version);
		}
		return this;
	}

	public CyrusIndexHeader to(ByteBuf buf) {
		if (version < 13) {
			throw new UnknownVersion("unsupported version " + version);
		}
		int currentPosition = buf.writerIndex();
		buf.writeInt(generation);
		buf.writeInt(format);
		buf.writeInt(version);
		buf.writeInt(startOffset);
		buf.writeInt(recordSize);
		buf.writeInt(numRecords);
		buf.writeInt((int) lastAppendDate);
		buf.writeInt(lastUid);
		buf.writeLong(quotaUsed);
		buf.writeInt((int) pop3LastLogin);
		buf.writeInt(uidValidity);
		buf.writeInt(deleted);
		buf.writeInt(answered);
		buf.writeInt(flagged);
		buf.writeBytes(options);
		buf.writeInt(leakedCache);
		buf.writeLong(highestModseq);
		buf.writeLong(deletedModseq);
		buf.writeInt(exists);
		buf.writeInt((int) firstExpunged);
		buf.writeInt((int) lastCleanup);
		buf.writeInt(headerFileCRC);
		buf.writeInt(syncCRCsBasic);
		buf.writeInt(recentUid);
		buf.writeInt((int) recentTime);
		buf.writeInt(pop3ShowAfter);
		buf.writeInt(quotaAnnotUsed);
		buf.writeInt(syncCRCsAnnot);
		if (version >= 14) {
			buf.writeInt(unseen);
			buf.writeBytes(new byte[28]); // spare fields
		}

		CRC32 crc = new CRC32();
		byte[] payloadbytes = new byte[CyrusIndexHeader.getHeaderSize(version) - 4];
		buf.getBytes(currentPosition, payloadbytes, 0, payloadbytes.length);
		crc.update(payloadbytes);
		headerCrc = (int) crc.getValue();
		buf.writeInt(headerCrc);
		return this;
	}

	public static CyrusIndexHeader from(InputStream stream) throws IOException, UnknownVersion {
		// Max header size is 160 bytes in version 15
		// read header start to get the header version
		ByteBuf buf = Unpooled.buffer();
		buf.writeBytes(stream, 12);
		int version = buf.getInt(8);

		int fullHeaderSize = getHeaderSize(version);
		int recordSize = getRecordSize(version);

		if (logger.isDebugEnabled()) {
			logger.debug("version: {} fullHeaderSize: {} recordSize: {}", version, fullHeaderSize, recordSize);
		}
		buf.writeBytes(stream, fullHeaderSize - 12);
		CyrusIndexHeader hdr = new CyrusIndexHeader(version).from(buf);
		return hdr;
	}

	public static int getHeaderSize(int version) throws UnknownVersion {
		switch (version) {
		case 9:
		case 10:
		case 11:
			return 96;
		case 12:
		case 13:
			return 128;
		case 14:
		case 15:
			return 160;
		default:
			logger.error("Unknown cyrus index version: {}", version);
			throw new UnknownVersion("unknown version " + version);
		}
	}

	public static int getRecordSize(int version) {
		switch (version) {
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
			return 80;
		case 10:
			return 88;
		case 11:
		case 12:
			return 96;
		case 13:
		case 14:
		case 15:
			return 104;
		default:
			logger.error("Unknown cyrus index version: {}", version);
			throw new UnknownVersion("unknown version " + version);
		}
	}
}
