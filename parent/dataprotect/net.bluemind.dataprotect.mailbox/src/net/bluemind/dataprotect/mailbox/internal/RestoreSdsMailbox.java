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
package net.bluemind.dataprotect.mailbox.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestoreSdsMailbox {
	private static final Logger logger = LoggerFactory.getLogger(RestoreSdsMailbox.class);
	private static final Path BACKUP_PATH = Paths.get("/var/backups/bluemind/sds");
	private CyrusSdsIndexReader sdsIndex;

	public RestoreSdsMailbox(Path jsondir) throws IOException {
		sdsIndex = new CyrusSdsIndexReader(jsondir.resolve("index.json"));
	}

	public RestoreSdsMailbox() throws IOException {
		this(BACKUP_PATH);
	}

	public CyrusSdsBackupMailbox getMailbox(String mailboxUid) throws IOException, ParseException {
		Path p = sdsIndex.getMailbox(mailboxUid);
		if (p == null) {
			logger.error("Unable to find mailbox uid {}", mailboxUid);
			return null;
		}
		return new CyrusSdsBackupMailbox(p.toFile());
	}
}
