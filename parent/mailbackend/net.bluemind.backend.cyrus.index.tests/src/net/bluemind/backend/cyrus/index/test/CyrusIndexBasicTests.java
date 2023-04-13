package net.bluemind.backend.cyrus.index.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.backend.cyrus.index.CyrusIndex;
import net.bluemind.backend.cyrus.index.CyrusIndexHeader;
import net.bluemind.backend.cyrus.index.CyrusIndexRecord;
import net.bluemind.backend.cyrus.index.UnknownVersion;

public class CyrusIndexBasicTests {
	@Test
	public void testBasicRead() throws IOException, UnknownVersion {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/cyrus.index")) {
			assertNotNull(in);
			// System.err.println("index: " + indextest);
			CyrusIndex index = new CyrusIndex(in);
			List<CyrusIndexRecord> records = index.readAll();
			CyrusIndexHeader header = index.getHeader();
			System.err.println("index header: " + header);

			assertEquals(10166, header.numRecords);
			assertEquals(10166, records.size());
			assertEquals(13, header.version);

			CyrusIndexRecord rec = records.get(0);
			System.err.println("record[0]: " + rec);
			assertEquals(1, rec.uid);
			assertEquals("024b110ad817ad6812141344764a36eba8a1ae50", rec.guid);
			assertEquals(156313, rec.size);
		}
	}

	@Test
	public void testBasicWriteCRC() throws IOException, UnknownVersion {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/cyrus.index")) {
			assertNotNull(in);
			CyrusIndex index = new CyrusIndex(in);
			List<CyrusIndexRecord> records = index.readAll();
			ByteBuf buf = Unpooled.buffer();
			for (CyrusIndexRecord readrec : records) {
				readrec.to(buf);
				CyrusIndexRecord newrec = CyrusIndexRecord.from(index.getHeader().version, buf);
				assertEquals(readrec.recordCRC, newrec.recordCRC);
			}
		}
	}
}
