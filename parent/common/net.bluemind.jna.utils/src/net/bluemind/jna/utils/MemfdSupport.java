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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;

public class MemfdSupport {

	private static final Logger logger = LoggerFactory.getLogger(MemfdSupport.class);
	private static final CLibrary LIBC = init();

	private static final Set<OffHeapFinalyser> activeRefs = ConcurrentHashMap.newKeySet();
	private static final ReferenceQueue<OffHeapTemporaryFile> cleaners = new ReferenceQueue<>();
	private static final LongAdder needCleanup = new LongAdder();

	private interface CLibrary extends Library {

		public int memfd_create(String name, int flags) throws LastErrorException;// NOSONAR

		public int close(int fd) throws LastErrorException;

	}

	private static CLibrary init() {
		CLibrary lib = null;
		try {
			var tmp = Native.load("c", CLibrary.class);
			int memFd = tmp.memfd_create("check", 0);
			if (memFd != -1) {
				tmp.close(memFd);
				lib = tmp;
				logger.info("Enabling memfd_create support for off-heap temporary files");
			}
		} catch (LastErrorException e) {
			logger.warn("memfd_create test call failed {}, not enabling OffHeapTemporaryFile support", e.getMessage());
		} catch (NoClassDefFoundError e) {
			logger.warn("JNA not found. Native methods will be disabled.", e);
		} catch (UnsatisfiedLinkError e) {
			logger.error("Failed to link the C library against JNA. Native methods will be unavailable.", e);
		} catch (NoSuchMethodError e) {
			logger.warn("Obsolete version of JNA present", e);
		}
		return lib;
	}

	public static boolean isAvailable() {
		return LIBC != null;
	}

	/**
	 * Creates an offheap-memory backed temporary file.
	 * 
	 * @param name
	 * @return
	 */
	public static OffHeapTemporaryFile newOffHeapTemporaryFile(String name) {
		Objects.requireNonNull(name, "name must not be null");
		OffHeapTemporaryFile ret = null;

		cleanupStaleRefs();

		if (LIBC != null) {
			try {
				int result = LIBC.memfd_create(name, 0);
				logger.debug("memfd_create({}) => {}", name, result);
				if (result != -1) {
					var cleaner = new OneTimeClose(name, () -> LIBC.close(result));
					ret = new OffHeapTemporaryFile(result, cleaner);
					OffHeapFinalyser ref = new OffHeapFinalyser(cleaner, ret, cleaners);
					activeRefs.add(ref);
				}
			} catch (LastErrorException errno) {
				logger.warn("memfd_create failed", errno);
			}
		} else {
			logger.warn("memfd_create method is not bound.");
		}
		return ret;

	}

	public static void resetAutoReclaimCount() {
		needCleanup.reset();
	}

	public static long autoReclaimed() {
		return needCleanup.sum();
	}

	private static void cleanupStaleRefs() {
		activeRefs.removeIf(OffHeapFinalyser::isGone);
		Reference<?> ref;
		while ((ref = cleaners.poll()) != null) {
			if (ref instanceof OffHeapFinalyser ohf) {
				boolean cleanup = ohf.finalizeResources();
				if (cleanup) {
					needCleanup.increment();
				}
				ohf.clear();
			}
		}
	}

}
