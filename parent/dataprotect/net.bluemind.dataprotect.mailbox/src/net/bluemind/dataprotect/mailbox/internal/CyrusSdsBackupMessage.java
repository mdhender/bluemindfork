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
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class CyrusSdsBackupMessage {
	public String guid;
	public Date date;

	public CyrusSdsBackupMessage(String guid, Date date) {
		this.guid = guid;
		this.date = date;
	}

	public static CyrusSdsBackupMessage from(SimpleDateFormat datefmt, JsonParser parser)
			throws IOException, ParseException {
		Date date = null;
		String guid = null;

		if (parser.currentToken() != JsonToken.START_OBJECT) {
			throw new IllegalStateException("Expected an object");
		}
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String fieldName = parser.getCurrentName();
			switch (fieldName) {
			case "g":
				guid = parser.nextTextValue();
				break;
			case "d":
				date = datefmt.parse(parser.nextTextValue());
				break;
			default:
				break;
			}
		}
		if (guid != null && date != null) {
			return new CyrusSdsBackupMessage(guid, date);
		} else {
			return null;
		}
	}
}