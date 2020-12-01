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
package net.bluemind.cli.user;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import net.bluemind.cli.calendar.ExportCalendarCommand;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.contact.ExportAddressBookCommand;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.cli.todolist.ExportTodolistCommand;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.user.api.IUser;
import net.bluemind.utils.FileUtils;
import picocli.CommandLine.Command;

@Command(name = "export", description = "export user data to an archive file")
public class UserExportCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserExportCommand.class;
		}
	}

	public String outputDir = "/tmp/bm-export";
	public String rootDir = "/tmp/bm-export";

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		outputDir = rootDir + "/" + UUID.randomUUID(); // BM-15290: Needed when using --match [a-c].*
		File dir = new File(outputDir);
		try {
			dir.mkdirs();
			Arrays.asList("contact", "calendar", "task").forEach(data -> exportData(de, data));

			createEmailSymlink(domainUid, de);

			ctx.info("Creating archive file, can take a moment...");
			File archiveFile = createArchive(de);
			ctx.info("Archive file for " + de.value.email + " created as : " + archiveFile.getAbsolutePath());
		} finally {
			FileUtils.delete(dir);
		}

	}

	private File createArchive(ItemValue<DirEntry> de) {
		File archiveFile = new File(rootDir + "/" + de.value.email + ".tgz");

		try (OutputStream fOut = Files.newOutputStream(archiveFile.toPath());
				BufferedOutputStream bOut = new BufferedOutputStream(fOut);
				GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(bOut);
				TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut)) {
			tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
			tOut.setAddPaxHeadersForNonAsciiNames(true);
			addFileToTarGz(tOut, outputDir, de.value.email);
		} catch (Exception e) {
			throw new CliException("Error generating archive file", e);
		}

		return archiveFile;
	}

	private void addFileToTarGz(TarArchiveOutputStream tOut, String path, String base) throws IOException {
		// resolve path
		path = new File(path).getCanonicalPath();

		File f = new File(path);
		TarArchiveEntry tarEntry = new TarArchiveEntry(f, base);
		tOut.putArchiveEntry(tarEntry);

		if (f.isFile()) {
			try (InputStream in = Files.newInputStream(f.toPath())) {
				IOUtils.copy(in, tOut);
				tOut.closeArchiveEntry();
			}
		} else {
			tOut.closeArchiveEntry();
			File[] children = f.listFiles();
			if (children != null) {
				for (File child : children) {
					addFileToTarGz(tOut, child.getCanonicalPath(), base + "/" + child.getName());
				}
			}
		}
	}

	private void exportData(ItemValue<DirEntry> de, String dataType) {
		File outputDataDir = prepareTmpDir(dataType);
		try {
			switch (dataType) {
			case "calendar":
				ExportCalendarCommand calendarExportCommand = new ExportCalendarCommand();
				calendarExportCommand.target = de.value.email;
				calendarExportCommand.rootDir = outputDataDir.getAbsolutePath();
				calendarExportCommand.forContext(ctx);
				calendarExportCommand.run();
				break;
			case "contact":
				ExportAddressBookCommand abExportCommand = new ExportAddressBookCommand();
				abExportCommand.target = de.value.email;
				abExportCommand.rootDir = outputDataDir.getAbsolutePath();
				abExportCommand.forContext(ctx);
				abExportCommand.run();
				break;
			case "task":
				ExportTodolistCommand todoExportCommand = new ExportTodolistCommand();
				todoExportCommand.target = de.value.email;
				todoExportCommand.rootDir = outputDataDir.getAbsolutePath();
				todoExportCommand.forContext(ctx);
				todoExportCommand.run();
				break;
			}
		} catch (Exception e) {
			throw new CliException("Error when exporting " + dataType, e);
		}
	}

	private void createEmailSymlink(String domainUid, ItemValue<DirEntry> de) {
		File outputMailDir = prepareTmpDir("mail");
		Path outputDataDir = Paths.get(outputMailDir.getAbsolutePath(), "data");
		Path outputMetaDir = Paths.get(outputMailDir.getAbsolutePath(), "meta");

		String login = ctx.adminApi().instance(IUser.class, domainUid).getComplete(de.uid).value.login;

		String cyrusPath = de.value.dataLocation + "__" + domainUid.replace('.', '_') + "/domain/" + domainUid.charAt(0)
				+ "/" + domainUid + "/" + firstLetterMailbox(login) + "/user/" + login.replace('.', '^');
		String cyrusData = "/var/spool/cyrus/data/" + cyrusPath;
		String cyrusMeta = "/var/spool/cyrus/meta/" + cyrusPath;

		try {
			Files.createSymbolicLink(outputDataDir, new File(cyrusData).toPath());
			Files.createSymbolicLink(outputMetaDir, new File(cyrusMeta).toPath());
		} catch (Exception e) {
			throw new CliException("Error when exporting mail", e);
		}
	}

	private char firstLetterMailbox(String mbox) {
		Character c = mbox.charAt(0);
		if (Character.isDigit(c)) {
			return 'q';
		} else {
			return c.charValue();
		}
	}

	private File prepareTmpDir(String dataType) {
		File dir = new File(outputDir + "/" + dataType);
		dir.mkdir();
		return dir;
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}