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
package net.bluemind.webmodule.uploadhandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;

public class TemporaryUploadRepository {

	private static final Logger logger = LoggerFactory.getLogger(TemporaryUploadRepository.class);
	private final File rootPath;
	private final Vertx vertx;

	public static class UniqueFile {
		public final UUID uuid;
		public final File file;

		public UniqueFile(UUID uuid, File f) {
			this.uuid = uuid;
			this.file = f;
		}
	}

	public TemporaryUploadRepository(Vertx vertx) {
		this.vertx = vertx;
		rootPath = new File("/tmp/tmpUpload");
		rootPath.mkdirs();
	}

	public UniqueFile createTempFile() {
		UUID random = UUID.randomUUID();
		File tmp = new File(rootPath, random.toString());
		// delete temp files
		// duration 10 minutes
		vertx.setTimer(1000 * 60 * 10, new Handler<Long>() {

			@Override
			public void handle(Long arg0) {
				try {
					Files.delete(getTempFile(random).toPath());
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
		return new UniqueFile(random, tmp);
	}

	public File getTempFile(UUID random) {
		return new File(rootPath, random.toString());
	}
}
