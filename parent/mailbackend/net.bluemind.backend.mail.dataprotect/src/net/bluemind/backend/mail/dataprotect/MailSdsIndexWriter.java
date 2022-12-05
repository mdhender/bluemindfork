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
package net.bluemind.backend.mail.dataprotect;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class MailSdsIndexWriter implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(MailSdsIndexWriter.class);

	private JsonGenerator generator;

	public MailSdsIndexWriter(Path indexPath) throws IOException {
		Set<PosixFilePermission> backupPermissions = PosixFilePermissions.fromString("rw-------");
		OutputStream outStream = Files.newOutputStream(indexPath, StandardOpenOption.CREATE_NEW,
				StandardOpenOption.TRUNCATE_EXISTING);
		generator = new JsonFactory().createGenerator(outStream, JsonEncoding.UTF8);
		Files.setPosixFilePermissions(indexPath, backupPermissions);
		generator.writeStartArray();
	}

	public void add(String mailboxUid, Path filename) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("mailboxUid", mailboxUid);
		generator.writeStringField("filename", filename.getFileName().toString());
		generator.writeEndObject();
	}

	@Override
	public void close() {
		if (generator != null) {
			try {
				generator.writeEndArray();
				generator.close();
			} catch (IOException e) {
				logger.error("Error trying to close index file: {}", e.getMessage(), e);
			}
		}
	}
}