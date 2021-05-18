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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;
import com.google.common.io.Files;

public class DataLocation {

	private static final Logger logger = LoggerFactory.getLogger(DataLocation.class);
	private static final String PATH = "/etc/bm/server.uid";

	private static final Supplier<String> dataLoc = Suppliers.memoizeWithExpiration(DataLocation::currentImpl, 1,
			TimeUnit.MINUTES);

	private DataLocation() {
	}

	/**
	 * Returns the data location of the running BlueMind JVM, based on
	 * <code>/etc/bm/server.uid</code>
	 * 
	 * <code>unknown</code> is returned if the file is missing.
	 * 
	 * @return
	 */
	public static String current() {
		return dataLoc.get();
	}

	private static String currentImpl() {
		try {
			return Files.asCharSource(new File(PATH), StandardCharsets.US_ASCII).read();
		} catch (IOException e) {
			logger.warn("Error figuring out current location ({}) => unknown", e.getMessage());
			return "unknown";
		}
	}

}
