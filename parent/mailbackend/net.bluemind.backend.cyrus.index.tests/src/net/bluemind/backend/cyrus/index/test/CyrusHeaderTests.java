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
package net.bluemind.backend.cyrus.index.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.ByteStreams;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.backend.cyrus.index.CyrusHeader;
import net.bluemind.backend.cyrus.index.CyrusHeader.CyrusAcl;

public class CyrusHeaderTests {
	@Test
	public void CyrusHeaderRead() throws IOException {
		ByteBuf buf = Unpooled.buffer();
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/cyrus.header")) {
			buf.writeBytes(ByteStreams.toByteArray(in));
		}
		CyrusHeader hdr = CyrusHeader.from(buf);
		System.err.println("hdr: " + hdr);
		assertEquals("489daff7.internal!user.laurent", hdr.quotaRoot());
		assertEquals("8b1b53b4-51cb-439b-8088-118735fa2248", hdr.uniqueId());
		assertEquals(2, hdr.acls().size());
		assertEquals("admin0", hdr.acls().get(0).username);
		assertEquals("lrswipkxtecdan", hdr.acls().get(0).acls);
		assertEquals("cli-created-ff74e83c-1fad-49a9-8fca-89e767d2bdd7@489daff7.internal", hdr.acls().get(1).username);
		assertEquals("lrswipkxtecdan", hdr.acls().get(1).acls);
	}

	@Test
	public void CyrusHeaderWrite() throws IOException {
		ByteBuf buf = Unpooled.buffer();
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/cyrus.header")) {
			buf.writeBytes(ByteStreams.toByteArray(in));
		}
		CyrusHeader hdr = CyrusHeader.from(buf);

		CyrusHeader newhdr = new CyrusHeader();
		newhdr.quotaRoot(hdr.quotaRoot());
		newhdr.uniqueId(hdr.uniqueId());
		newhdr.flags(hdr.flags());
		newhdr.acls(hdr.acls().toArray(new CyrusAcl[0]));

		ByteBuf newbuf = Unpooled.buffer();
		newhdr.to(newbuf);
		buf.resetReaderIndex();
		byte[] original = new byte[buf.readableBytes()];
		byte[] newheader = new byte[newbuf.readableBytes()];
		buf.readBytes(original);
		newbuf.readBytes(newheader);
//		System.err.println(ByteBufUtil.prettyHexDump(buf));
//		System.err.println(ByteBufUtil.prettyHexDump(newbuf));
		Assert.assertArrayEquals(original, newheader);
	}

}
