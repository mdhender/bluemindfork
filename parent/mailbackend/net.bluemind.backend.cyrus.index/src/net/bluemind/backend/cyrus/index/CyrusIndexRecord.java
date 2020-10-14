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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

public class CyrusIndexRecord {
	public final int version;

	public int uid; // int32 4
	public long internalDate; // time_t 4
	public long sentDate; // time_t 4
	public int size; // int32 4
	public int headerSize; // int32 4
	public long hmTime; // time_t 4
	public int cacheOffset; // int32 4
	public long lastUpdated; // time_t 4
	public final byte[] systemFlags = new byte[4]; // bitmap 4
	public final byte[] userFlags = new byte[16]; // bitmap 16
	public long saveDate; // time_t 4
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

	public static CyrusIndexRecord fromBuffer(int version, ByteBuf buf) {
		CyrusIndexRecord record = new CyrusIndexRecord(version);
		record.uid = buf.readInt();
		record.internalDate = buf.readInt();
		record.sentDate = buf.readUnsignedInt();
		record.size = buf.readInt();
		record.headerSize = buf.readInt();
		record.hmTime = buf.readUnsignedInt();
		record.cacheOffset = buf.readInt();
		record.lastUpdated = buf.readUnsignedInt();
		buf.readBytes(record.systemFlags);
		buf.readBytes(record.userFlags);
		record.saveDate = buf.readUnsignedInt();
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

}
