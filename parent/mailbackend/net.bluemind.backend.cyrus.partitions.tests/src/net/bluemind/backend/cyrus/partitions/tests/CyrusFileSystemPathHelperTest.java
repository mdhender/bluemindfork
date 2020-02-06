package net.bluemind.backend.cyrus.partitions.tests;

import static org.junit.Assert.assertEquals;

import java.util.stream.IntStream;

import org.junit.Test;

import net.bluemind.backend.cyrus.partitions.CyrusFileSystemPathHelper;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;

public class CyrusFileSystemPathHelperTest {
	@Test
	public void getDomainDataFileSystemPath_firstIsLetter() {
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "bm.lan");
		String path = CyrusFileSystemPathHelper.getDomainDataFileSystemPath(partition, "bm.lan");
		assertEquals("/var/spool/cyrus/data/srv__bm_lan/domain/b/bm.lan", path);
	}

	@Test
	public void getDomainDataFileSystemPath_firstIsNotLetter() {
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "1bm.lan");
		String path = CyrusFileSystemPathHelper.getDomainDataFileSystemPath(partition, "1bm.lan");
		assertEquals("/var/spool/cyrus/data/srv__1bm_lan/domain/q/1bm.lan", path);
	}

	@Test
	public void getDomainMetaFileSystemPath_firstIsLetter() {
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "bm.lan");
		String path = CyrusFileSystemPathHelper.getDomainMetaFileSystemPath(partition, "bm.lan");
		assertEquals("/var/spool/cyrus/meta/srv__bm_lan/domain/b/bm.lan", path);
	}

	@Test
	public void getDomainMetaFileSystemPath_firstIsNotLetter() {
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "1bm.lan");
		String path = CyrusFileSystemPathHelper.getDomainMetaFileSystemPath(partition, "1bm.lan");
		assertEquals("/var/spool/cyrus/meta/srv__1bm_lan/domain/q/1bm.lan", path);
	}

	@Test
	public void getDomainHSMFileSystemPath_firstIsLetter() {
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "bm.lan");
		String path = CyrusFileSystemPathHelper.getDomainHSMFileSystemPath(partition, "bm.lan");
		assertEquals("/var/spool/bm-hsm/cyrus-archives/srv__bm_lan/domain/b/bm.lan", path);
	}

	@Test
	public void getDomainHSMFileSystemPath_firstIsNotLetter() {
		CyrusPartition partition = CyrusPartition.forServerAndDomain("srv", "1bm.lan");
		String path = CyrusFileSystemPathHelper.getDomainHSMFileSystemPath(partition, "1bm.lan");
		assertEquals("/var/spool/bm-hsm/cyrus-archives/srv__1bm_lan/domain/q/1bm.lan", path);
	}

	@Test
	public void mapLetter_isLetter() {
		IntStream.rangeClosed('a', 'z').forEach(letterAsInt -> {
			char letter = (char) letterAsInt;
			String domainUid = String.format("%sdomain.lan", letter);
			assertEquals(letter, CyrusFileSystemPathHelper.mapLetter(domainUid.charAt(0)));

			domainUid = String.format("%sdomain.lan", Character.toUpperCase(letter));
			assertEquals(letter, CyrusFileSystemPathHelper.mapLetter(domainUid.charAt(0)));
		});
	}

	@Test
	public void mapLetter_isNotLetter() {
		for (int i = 0; i < 10; i++) {
			String domainUid = String.format("%sdomain.lan", i);
			assertEquals('q', CyrusFileSystemPathHelper.mapLetter(domainUid.charAt(0)));
		}

		assertEquals('q', CyrusFileSystemPathHelper.mapLetter('_'));
	}
}
