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
package net.bluemind.backend.mail.parsing;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bodies {

	private static final Logger logger = LoggerFactory.getLogger(Bodies.class);

	private static final File STAGING = new File("/var/spool/bm-mail/bodies");

	static {
		STAGING.mkdirs();
	}

	public static File getFolder(String sid) {
		File sidFolder = new File(STAGING, sid);
		if (!sidFolder.exists()) {
			sidFolder.mkdir();
			logger.debug("Folder " + sidFolder.getAbsolutePath() + " created.");
		}
		return sidFolder;
	}

	private Bodies() {
	}

}
