/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.milter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MilterInstanceID {

	private static final String ETC_BM_MCAST_ID = "/etc/bm/mcast.id";
	private static final Logger logger = LoggerFactory.getLogger(MilterInstanceID.class);
	private static String id;

	private MilterInstanceID() {

	}

	public static String get() {
		if (id == null) {
			File mcastIdFile = new File(ETC_BM_MCAST_ID);
			if (mcastIdFile.exists()) {
				try {
					id = Files.readString(mcastIdFile.toPath(), Charset.defaultCharset());
					return id;
				} catch (IOException e) {
					logger.warn("Cannot read mcast.id", e);
				}
			}
		} else {
			return id;
		}
		return UUID.randomUUID().toString();
	}

}
