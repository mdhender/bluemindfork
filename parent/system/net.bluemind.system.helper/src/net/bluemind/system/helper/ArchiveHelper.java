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
package net.bluemind.system.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.system.helper.distrib.list.Distribution;

public class ArchiveHelper {

	private static final Logger logger = LoggerFactory.getLogger(ArchiveHelper.class);

	public static final String SUBSCRIPTION_ARCHIVE_PATH = "/etc/bm/subscription.bmz";
	public static final int SUBSCRIPTION_ARCHIVE_MAX_SIZE = 1024 * 1024; // 1Mo

	public static void checkFileSize(File file) {
		if (file.length() > SUBSCRIPTION_ARCHIVE_MAX_SIZE) {
			file.delete();
			throw new ServerFault("Archive file size is too big. Size is limited to " + SUBSCRIPTION_ARCHIVE_MAX_SIZE,
					ErrorCode.FAILURE);
		}
	}

	public static byte[] getSubscriptionFile(File file, Distribution serverOS) {
		try (ZipFile zipFile = new ZipFile(file)) {
			return parseZipFile(zipFile, serverOS);
		} catch (IOException e) {
			logger.error("Failed to read directly the zip file, will try to decode it.");
			try (ZipFile zipFile = decodeBase64(file)) {
				return parseZipFile(zipFile, serverOS);
			} catch (IOException exc) {
				throw new ServerFault("Failed to read the zip file." + exc);
			}
		}
	}

	private static byte[] parseZipFile(ZipFile zipFile, Distribution serverOS) throws IOException {
		Optional<? extends ZipEntry> optSubscriptionFile = zipFile.stream()
				.filter(zipEntry -> zipEntry.getName().equals(serverOS.getName())).findFirst();

		if (optSubscriptionFile.isPresent()) {
			logger.info("Subscription file found in zip file.");
			return ByteStreams.toByteArray(zipFile.getInputStream(optSubscriptionFile.get()));
		}

		throw new ServerFault("No subscription file matching server OS found in archive", ErrorCode.FAILURE);

	}

	private static ZipFile decodeBase64(File file) throws IOException {
		byte[] before = Files.readAllBytes(file.toPath());

		// delete first and last byte corresponding to JSON characters {}
		byte[] cleaned = Arrays.copyOfRange(before, 1, (int) file.length() - 1);

		byte[] after = Base64.getDecoder().decode(cleaned);

		Files.write(file.toPath(), after);
		return new ZipFile(file);
	}

}
