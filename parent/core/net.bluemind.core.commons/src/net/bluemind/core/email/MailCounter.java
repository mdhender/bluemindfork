/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.core.email;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

public class MailCounter {

	public static void count(File file, Set<Long> files) throws IOException {
		if (!file.exists()) {
			return;
		}
		Path startingDir = file.toPath();
		FileVisitor fileVisitor = new FileVisitor(files);
		Files.walkFileTree(startingDir, fileVisitor);
	}

	private static class FileVisitor extends SimpleFileVisitor<Path> {

		private Set<Long> files = new HashSet<>();

		public FileVisitor(Set<Long> files) {
			this.files = files;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

			if (file.toFile().getName().endsWith(".")) {
				String filekey = attrs.fileKey().toString();
				String ino = filekey.substring(filekey.lastIndexOf('=') + 1, filekey.length() - 1);
				files.add(Long.parseLong(ino));
			}

			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			return FileVisitResult.CONTINUE;
		}
	}

}
