/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.tika.server.impl;

import java.io.File;

public class TikaDirectories {

	public static final File SPOOL_ROOT = new File("/var/spool/bm-tika");

	public static final File CACHED_TEXTS = new File(SPOOL_ROOT, "cache");

	public static final File WORK = new File(SPOOL_ROOT, "work");

	static {
		CACHED_TEXTS.mkdirs();
		WORK.mkdirs();
	}

}
