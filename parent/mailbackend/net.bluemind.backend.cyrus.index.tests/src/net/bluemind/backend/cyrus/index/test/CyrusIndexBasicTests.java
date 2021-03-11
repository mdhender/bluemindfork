package net.bluemind.backend.cyrus.index.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.bluemind.backend.cyrus.index.CyrusIndex;
import net.bluemind.backend.cyrus.index.CyrusIndexHeader;
import net.bluemind.backend.cyrus.index.CyrusIndexMailboxRecord;
import net.bluemind.backend.cyrus.index.CyrusIndexRecord;
import net.bluemind.backend.cyrus.index.CyrusIndexWriter;
import net.bluemind.backend.cyrus.index.UnknownVersion;
import net.bluemind.backend.mail.api.flags.MailboxItemFlag;
import net.bluemind.backend.mail.replica.api.MailboxReplica;

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

	@Test
	public void testWriteFromMailboxRecord() throws IOException {
		MailboxReplica folder = new MailboxReplica();
		folder.lastUid = 42;
		folder.lastAppendDate = Date
				.from(LocalDate.of(2021, 2, 24).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
		folder.pop3LastLogin = Date
				.from(LocalDate.of(2021, 2, 23).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
		folder.recentTime = Date
				.from(LocalDate.of(2021, 2, 22).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
		folder.highestModSeq = 42;
		folder.syncCRC = 0;
		folder.deleted = false;

		CyrusIndexMailboxRecord record1 = new CyrusIndexMailboxRecord();
		record1.guid("50d7436039744c253f9b2a4e90cbedb02ebfb82d").size(5).imapUid(8);
		record1.modSeq(0).lastUpdated(new Date()).internalDate(new Date()).sentDate(new Date());
		record1.systemFlags(MailboxItemFlag.System.Seen.value().value | MailboxItemFlag.System.Flagged.value().value);

		CyrusIndexMailboxRecord record2 = new CyrusIndexMailboxRecord();
		record2.guid("f722f20fc568981ad1702f8075048e08a766bfa0").size(4).imapUid(42);
		record2.modSeq(1).lastUpdated(new Date()).internalDate(new Date()).sentDate(new Date());
		record2.systemFlags(0);

		ArrayList<CyrusIndexMailboxRecord> records = new ArrayList<>(2);
		records.add(record1);
		records.add(record2);

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			CyrusIndexWriter writer = new CyrusIndexWriter(out, 13);
			writer.writeAllRecords(folder, records, records.size());
			System.err.println("output: " + ByteBufUtil.hexDump(out.toByteArray()));
		}
		try (OutputStream out = Files.newOutputStream(Paths.get("/tmp/cyrus.index"),
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			CyrusIndexWriter writer = new CyrusIndexWriter(out, 13);
			writer.writeAllRecords(folder, records, records.size());
		}
	}
}
