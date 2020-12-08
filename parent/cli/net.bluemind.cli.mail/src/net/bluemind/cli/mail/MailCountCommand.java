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

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import picocli.CommandLine.Command;

@Command(name = "count", description = "List the number of unique messages in the cyrus spool on the current server")
public class MailCountCommand implements ICmdLet, Runnable {
	private CliContext ctx;

	@Override
	public void run() {
		File spool = new File("/var/spool/cyrus/data");
		File hsm = new File("/var/spool/bm-hsm/cyrus-archives");
		Set<Long> files = new HashSet<>();
		count(spool, files);
		long spoolCount = files.size();
		count(hsm, files);
		long hsmCount = files.size() - spoolCount;
		long total = files.size();
		ctx.info("Found " + total + " mails. Spool: " + spoolCount + ", HSM: " + hsmCount);
	}

	private void count(File file, Set<Long> files) {
		if (!file.exists()) {
			return;
		}
		Path startingDir = file.toPath();
		FileVisitor fileVisitor = new FileVisitor(files);
		try {
			Files.walkFileTree(startingDir, fileVisitor);
		} catch (IOException e) {
			throw new CliException(e);
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	private class FileVisitor extends SimpleFileVisitor<Path> {

		private Set<Long> files = new HashSet<>();

		public FileVisitor(Set<Long> files) {
			this.files = files;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

			if (file.toFile().getName().endsWith(".")) {
				try {
					String filekey = attrs.fileKey().toString();
					String ino = filekey.substring(filekey.lastIndexOf('=') + 1, filekey.length() - 1);
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
