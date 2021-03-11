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
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class CyrusIndex {
	private final InputStream stream;
	private CyrusIndexHeader header;

	public CyrusIndex(InputStream in) {
		stream = in;
	}

	public CyrusIndexHeader getHeader() {
		return header;
	}

	public List<CyrusIndexRecord> readAll() throws IOException, UnknownVersion {
		header = readHeader();
		ArrayList<CyrusIndexRecord> records = new ArrayList<>();
		if (header.version >= 12) {
			for (long i = 0; i < header.numRecords; i++) {
				ByteBuf buf = Unpooled.buffer(header.recordSize);
				buf.writeBytes(stream, header.recordSize);
				records.add(CyrusIndexRecord.from(header.version, buf));
			}
		}
		return records;
	}

	private CyrusIndexHeader readHeader() throws IOException, UnknownVersion {
		header = CyrusIndexHeader.from(stream);
		return header;
	}
}
