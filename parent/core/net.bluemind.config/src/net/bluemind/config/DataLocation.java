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
package net.bluemind.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.io.Files;

public class DataLocation {

	private static final String dataLoc = currentImpl();

	/**
	 * Returns the data location of the running BlueMind JVM
	 * 
	 * @return
	 */
	public static String current() {
		return dataLoc;
	}

	private static String currentImpl() {
		try {
			return Files.asCharSource(new File("/etc/bm/server.uid"), StandardCharsets.US_ASCII).read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
