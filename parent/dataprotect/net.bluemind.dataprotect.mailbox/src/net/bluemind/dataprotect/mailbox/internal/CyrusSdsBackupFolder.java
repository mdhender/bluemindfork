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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class CyrusSdsBackupFolder {
	private String uid;
	private String name;
	private String fullName;
	private List<CyrusSdsBackupMessage> messages = new ArrayList<>();

	public CyrusSdsBackupFolder(String uid, String name, String fullName) {
		this.uid = uid;
		this.fullName = fullName;
		this.name = name;
	}

	public List<CyrusSdsBackupMessage> messages() {
		return messages;
	}

	public int messageCount() {
		return messages.size();
	}

	public String toString() {
		return String.format("<CyrusSdsBackupFolder uid=%s name=%s fullName=%s>", uid, name, fullName);
	}

	public String uid() {
		return uid;
	}

	public String name() {
		return name;
	}

	public String fullName() {
		return fullName;
	}

	public String fullNameWithoutInbox() {
		return fullName.replace("INBOX", "");
	}

	public static CyrusSdsBackupFolder from(JsonParser parser) throws IOException, ParseException {
		if (parser.currentToken() != JsonToken.START_OBJECT) {
			throw new IllegalStateException("Expected an object");
		}
		String fullName = null;
		String uid = null;
		String name = null;
		CyrusSdsBackupFolder folder = null;

		SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		dateformat.setTimeZone(TimeZone.getTimeZone("GMT"));

		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();
			switch (fieldName) {
			case "fullName":
				fullName = parser.nextTextValue();
				if (uid != null && fullName != null && name != null) {
					folder = new CyrusSdsBackupFolder(uid, name, fullName);
				}
				break;
			case "name":
				name = parser.nextTextValue();
				if (uid != null && fullName != null && name != null) {
					folder = new CyrusSdsBackupFolder(uid, name, fullName);
				}
				break;
			case "uid":
				uid = parser.nextTextValue();
				if (uid != null && fullName != null && name != null) {
					folder = new CyrusSdsBackupFolder(uid, name, fullName);
				}
				break;
			case "messages":
				if (folder == null) {
					throw new IllegalStateException("expected folder to be fully defined");
				}
				parser.nextToken();
				if (parser.currentToken() != JsonToken.START_ARRAY) {
					throw new IllegalStateException("Expected an array");
				}
				while (parser.nextToken() != JsonToken.END_ARRAY) {
					CyrusSdsBackupMessage msg = CyrusSdsBackupMessage.from(dateformat, parser);
					if (msg != null) {
						folder.addMessage(msg);
					}
				}
				break;
			default:
				break;
			}

		}
		return folder;
	}

	public void addMessage(CyrusSdsBackupMessage msg) {
		messages.add(msg);
	}
}