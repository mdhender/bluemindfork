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
package net.bluemind.cli.mail;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.airlift.airline.Command;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;

@Command(name = "count", description = "List the number of unique messages in the cyrus spool on the current server")
public class MailCountCommand implements ICmdLet, Runnable {
	private CliContext ctx;

	@Override
	public void run() {
		File spool = new File("/var/spool/cyrus/data");
		Path startingDir = spool.toPath();
		FileVisitor fileVisitor = new FileVisitor();
		try {
			Files.walkFileTree(startingDir, fileVisitor);
		} catch (IOException e) {
			throw new CliException(e);
		}
		ctx.info("Found " + fileVisitor.getFileCount() + " mails in directory " + spool);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	private class FileVisitor extends SimpleFileVisitor<Path> {

		private Set<Long> files = new HashSet<>();

		int getFileCount() {
			return files.size();
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

			if (file.toFile().getName().endsWith(".")) {
				try {
					String filekey = attrs.fileKey().toString();
					String ino = filekey.substring(filekey.lastIndexOf("=") + 1, filekey.length() - 1);
					files.add(Long.parseLong(ino));
				} catch (Exception e) {
					ctx.error("Cannot retrieve ino attr of " + file.getFileName().endsWith(".eml"));
				}
			}

			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			ctx.info("failed " + file.toFile().getAbsolutePath() + " " + exc.getMessage());
			return FileVisitResult.CONTINUE;
		}
	}

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("mail");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return MailCountCommand.class;
		}
	}

}
