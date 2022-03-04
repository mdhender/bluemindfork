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
package net.bluemind.dataprotect.mailbox;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class CyrusSdsBackupMailbox implements AutoCloseable {
	private final JsonParser parser;
	private String domainUid;
	private String dataLocation;
	private String mailboxUid;
	private String kind;

	private int version;
	private List<CyrusSdsBackupFolder> folders = new ArrayList<>();

	public CyrusSdsBackupMailbox(File jsonfile) throws IOException, ParseException {
		JsonFactory jsonfactory = new JsonFactory();
		parser = jsonfactory.createParser(jsonfile);
		parser.nextToken();
		readHeader();
	}

	private void readHeader() throws IOException, ParseException {
		if (parser.currentToken() != JsonToken.START_OBJECT) {
			throw new IllegalStateException("Expected an object");
		}
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();
			if (fieldName == null) {
				continue;
			}
			switch (fieldName) {
			case "domainUid":
				domainUid = parser.nextTextValue();
				break;
			case "dataLocation":
				dataLocation = parser.nextTextValue();
				break;
			case "version":
				version = parser.nextIntValue(1);
				break;
			case "mailboxUid":
				mailboxUid = parser.nextTextValue();
				break;
			case "kind":
				kind = parser.nextTextValue();
				break;
			case "folders":
				parser.nextToken();
				if (parser.currentToken() != JsonToken.START_ARRAY) {
					throw new IllegalStateException("Expected an array");
				}
				while (parser.nextToken() != JsonToken.END_ARRAY) {
					CyrusSdsBackupFolder folder = CyrusSdsBackupFolder.from(parser);
					if (folder != null) {
						folders.add(folder);
					}
				}
				return;
			case "backingstore":
				// Trash this, not used
				parser.nextToken();
				parser.skipChildren();
				break;
			default:
				parser.nextToken();
				break;
			}
		}
	}

	public String domainUid() {
		return domainUid;
	}

	public String dataLocation() {
		return dataLocation;
	}

	public int version() {
		return version;
	}

	public String mailboxUid() {
		return mailboxUid;
	}

	public String kind() {
		return kind;
	}

	public List<CyrusSdsBackupFolder> getFolders() {
		return folders;
	}

	@Override
	public void close() throws Exception {
		if (this.parser != null) {
			this.parser.close();
		}
	}
}