/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.jna.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;

public class MemlockSupport {

	private static final Logger logger = LoggerFactory.getLogger(MemlockSupport.class);
	private static final CLibrary LIBC = init();

	/**
	 * Lock all currently mapped pages.
	 */
	private static final int MCL_CURRENT = 1;

	private interface CLibrary extends Library {

		public int mlockall(int flags) throws LastErrorException;

		public int munlockall() throws LastErrorException;
	}

	private static CLibrary init() {
		try {
			return Native.load("c", CLibrary.class);
		} catch (NoClassDefFoundError e) {
			logger.warn("JNA not found. Native methods will be disabled.", e);
		} catch (UnsatisfiedLinkError e) {
			logger.error("Failed to link the C library against JNA. Native methods will be unavailable.", e);
		} catch (NoSuchMethodError e) {
			logger.warn("Obsolete version of JNA present", e);
		}
		return null;
	}

	public static void mlockallOrWarn() {
		if (LIBC != null) {
			try {
				int result = LIBC.mlockall(MCL_CURRENT);
				logger.info("mlockall(MCL_CURRENT) => {}", result);
			} catch (LastErrorException errno) {
				logger.warn("mlock failed", errno);
			}
		} else {
			logger.warn("mlockall method is not bound.");
		}

	}

}
