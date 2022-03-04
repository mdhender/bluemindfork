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
package net.bluemind.dataprotect.cyrussdsindex.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import net.bluemind.dataprotect.mailbox.CyrusSdsBackupFolder;
import net.bluemind.dataprotect.mailbox.CyrusSdsBackupMailbox;
import net.bluemind.dataprotect.mailbox.CyrusSdsIndexReader;

public class CyrusSdsIndexTests {

	@Test
	public void readIndex() throws IOException {
		Path tempFile = Files.createTempFile("index-", ".json");
		Path parentDir = tempFile.getParent();
		Path mboxPath;
		try {
			try (InputStream sdsIndexIn = this.getClass().getClassLoader().getResourceAsStream("data/index.json")) {
				Files.copy(sdsIndexIn, tempFile, StandardCopyOption.REPLACE_EXISTING);
			}
			CyrusSdsIndexReader reader = new CyrusSdsIndexReader(tempFile);
			mboxPath = reader.getMailbox("cli-created-ff74e83c-1fad-49a9-8fca-89e767d2bdd7");
			assertEquals(parentDir.resolve("laurent.json"), mboxPath);
			mboxPath = reader.getMailbox("628B8025-3993-43FB-A4B0-18775862C16E");
			assertEquals(parentDir.resolve("michel.pick.json"), mboxPath);
			mboxPath = reader.getMailbox("non-existing");
			assertEquals(null, mboxPath);
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	public void readMailbox() throws Exception {
		Path tempFile = Files.createTempFile("michel.pick-", ".json");
		try {
			try (InputStream sdsIndexIn = this.getClass().getClassLoader()
					.getResourceAsStream("data/michel.pick.json")) {
				Files.copy(sdsIndexIn, tempFile, StandardCopyOption.REPLACE_EXISTING);
			}
			try (CyrusSdsBackupMailbox sdsMailbox = new CyrusSdsBackupMailbox(tempFile.toFile())) {
				assertEquals("bm-master", sdsMailbox.dataLocation());
				assertEquals("489daff7.internal", sdsMailbox.domainUid());
				List<CyrusSdsBackupFolder> folders = sdsMailbox.getFolders();
				assertEquals(folders.size(), 6);
				Optional<CyrusSdsBackupFolder> inbox = folders.stream().filter(f -> "INBOX".equals(f.fullName()))
						.findFirst();
				assertTrue(inbox.isPresent());
				assertEquals(4, inbox.get().messageCount());
				assertEquals("1115e492478b3968dd110d7f9b573c08bb31da44", inbox.get().messages().get(1).guid);
			}
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

}
