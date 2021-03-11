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

import java.util.Arrays;
import java.util.zip.CRC32;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class CyrusIndexRecord {
	public final int version;

	public int uid; // int32 4
	public long internalDate; // time_t 4
	public long sentDate; // time_t 4
	public int size; // int32 4
	public int headerSize; // int32 4
	public long gmTime; // time_t 4
	public int cacheOffset; // int32 4
	public long lastUpdated; // time_t 4
	public final byte[] systemFlags = new byte[4]; // bitmap 4
	public final byte[] userFlags = new byte[16]; // bitmap 16
	public long saveDate; // time_t 4
	public int contentLines; // int32 4
	public int cacheVersion; // int32 4
	public String guid; // hex 20
	public long modseq; // int64 8
	public String cid; // hex 8
	public int cacheCRC; // int32 4
	public int recordCRC; // int32 4

	public CyrusIndexRecord(int version) {
		this.version = version;
	}

	public String toString() {
		return String.format("<Record guid=%s uid=%s>", guid, uid);
	}

	public byte[] setSystemFlags(int systemFlags, String otherFlags) {
		Arrays.fill(this.systemFlags, (byte) 0);
//		int sysflags = 0;
//		for (MailboxItemFlag flag : inSysFlags) {
//			sysflags |= flag.value;
//		}
//		sysflags |= InternalFlag.valueOf(inIntFlags);
		this.systemFlags[0] = (byte) (systemFlags >> 24);
		this.systemFlags[1] = (byte) (systemFlags >> 16);
		this.systemFlags[2] = (byte) (systemFlags >> 8);
		this.systemFlags[3] = (byte) (systemFlags);
		return this.systemFlags;
	}

	public static CyrusIndexRecord from(int version, ByteBuf buf) {
		if (version < 13) {
			throw new UnknownVersion("unsupported version " + version);
		}
		CyrusIndexRecord record = new CyrusIndexRecord(version);
		record.uid = buf.readInt();
		record.internalDate = buf.readUnsignedInt();
		record.sentDate = buf.readUnsignedInt();
		record.size = buf.readInt();
		record.headerSize = buf.readInt();
		record.gmTime = buf.readUnsignedInt();
		record.cacheOffset = buf.readInt();
		record.lastUpdated = buf.readUnsignedInt();
		buf.readBytes(record.systemFlags);
		buf.readBytes(record.userFlags);
		if (version >= 15) {
			record.saveDate = buf.readInt();
		} else {
			record.contentLines = buf.readInt();
		}
		record.cacheVersion = buf.readInt();
		if (version <= 9) {
			byte[] bytesguid = new byte[12];
			buf.readBytes(bytesguid);
			record.guid = new String(bytesguid);
		} else {
			byte[] bytesguid = new byte[20];
			buf.readBytes(bytesguid);
			record.guid = ByteBufUtil.hexDump(bytesguid);
		}
		record.modseq = buf.readLong();
		if (version > 9) {
			if (version >= 13) {
				byte[] bytescid = new byte[8];
				buf.readBytes(bytescid);
				record.cid = ByteBufUtil.hexDump(bytescid);
			}
			record.cacheCRC = buf.readInt();
			record.recordCRC = buf.readInt();
		}
		return record;
	}

	public CyrusIndexRecord to(ByteBuf buf) {
		int currentPosition = buf.writerIndex();
		buf.writeInt(uid);
		buf.writeInt((int) internalDate);
		buf.writeInt((int) sentDate);
		buf.writeInt(size);
		buf.writeInt(headerSize);
		buf.writeInt((int) gmTime);
		buf.writeInt(cacheOffset);
		buf.writeInt((int) lastUpdated);
		buf.writeBytes(systemFlags);
		buf.writeBytes(userFlags);
		if (version >= 15) {
			buf.writeInt((int) saveDate);
		} else {
			buf.writeInt(contentLines);
		}
		buf.writeInt(cacheVersion);
		byte[] guidbytes;
		if (version <= 9) {
			guidbytes = guid.getBytes();
		} else {
			guidbytes = ByteBufUtil.decodeHexDump(guid);
		}
		buf.writeBytes(guidbytes);
		buf.writeLong(modseq);
		if (version > 9) {
			if (version >= 13) {
				byte[] bytescid = ByteBufUtil.decodeHexDump(cid);
				buf.writeBytes(bytescid);
			}
			buf.writeInt(cacheCRC);
			CRC32 crc = new CRC32();
			byte[] payloadbytes = new byte[CyrusIndexHeader.getRecordSize(version) - 4];
			buf.getBytes(currentPosition, payloadbytes, 0, payloadbytes.length);
			crc.update(payloadbytes);
			buf.writeInt((int) crc.getValue());
		}
		return this;
	}

}
