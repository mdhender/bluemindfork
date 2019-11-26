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
package net.bluemind.document.persistence.fs;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.document.storage.IDocumentStore;

public class FilesystemStore implements IDocumentStore {
	private static final Logger logger = LoggerFactory.getLogger(FilesystemStore.class);

	protected String STORAGE_DIR = Activator.getStorageDir();

	public FilesystemStore() {

	}

	@Override
	public void store(String uid, byte[] content) throws ServerFault {
		try {
			File file = getFile(uid);
			file.getParentFile().mkdirs();
			Files.write(file.toPath(), content);

			logger.info("File stored to {}", file.getAbsolutePath());

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e.getMessage(), e);
		}

	}

	@Override
	public byte[] get(String uid) throws ServerFault {

		File content = getFile(uid);
		if (!content.exists()) {
			logger.debug("Document {} not found", uid);
			return null;
		}

		try (InputStream in = Files.newInputStream(content.toPath())) {
			return ByteStreams.toByteArray(in);
		} catch (Exception t) {
			throw new ServerFault(t.getMessage(), t);
		}
	}

	@Override
	public void delete(String uid) throws ServerFault {
		File content = getFile(uid);
		if (!content.exists()) {
			logger.debug("Cannot delete document {}. File not found", uid);
			return;
		}

		logger.info("Delete file {}", content.getAbsolutePath());
		content.delete();
	}

	@Override
	public boolean exists(String uid) throws ServerFault {
		File content = getFile(uid);
		return content.exists();
	}

	@Override
	public int getPriority() {
		return 1;
	}

	private File getFile(String uid) {
		// FIXME check '../..' ?
		String path = STORAGE_DIR + "/" + uid + ".bin";
		return new File(path);

	}

}
