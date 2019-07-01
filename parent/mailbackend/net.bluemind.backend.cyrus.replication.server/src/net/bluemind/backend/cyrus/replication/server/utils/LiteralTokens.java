/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.cyrus.replication.server.utils;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.cyrus.replication.server.Token;

public class LiteralTokens {

	private static final Logger logger = LoggerFactory.getLogger(LiteralTokens.class);

	public static void export(Token t, File newName) {
		export(t.value(), newName);
	}

	public static void export(String contentTokenRef, File newName) {
		String fileName = contentTokenRef.substring(contentTokenRef.indexOf("{t") + 1, contentTokenRef.length() - 1);
		File f = new File(Token.ROOT, fileName);
		if (!f.exists()) {
			throw new RuntimeException("Missing token ref " + contentTokenRef);
		}
		if (newName.exists()) {
			logger.debug("Overriding {}", newName.getAbsolutePath());
		}
		f.renameTo(newName);
	}

}
