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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import net.bluemind.backend.mail.dataprotect.MailSdsBackup;
import net.bluemind.cli.calendar.ExportCalendarCommand;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.contact.ExportAddressBookCommand;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.cli.todolist.ExportTodolistCommand;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.dataprotect.sdsspool.SdsDataProtectSpool;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.network.topology.Topology;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.sds.store.loader.SdsStoreLoader;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;
import net.bluemind.utils.FileUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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

	@Option(names = { "-o",
			"--output-directory" }, defaultValue = "/tmp/bm-export", description = "output directory of exported users")
	public final Path exportDirectory = Paths.get("/tmp/bm-export");

	@Option(names = "--email-content", description = "download email messages content", negatable = true, defaultValue = "true", fallbackValue = "true")
	public final boolean downloadEmailContent = true;

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) {
		// BM-15290: Needed when using --match [a-c].*
		Path outputDir = exportDirectory.resolve(UUID.randomUUID().toString()).toAbsolutePath();
		ItemValue<Domain> domain = ctx.adminApi().instance(IDomains.class).get(domainUid);
		if (domain == null) {
			ctx.error("Unable to retrieve domain uid {}", domainUid);
		}
		File dir = outputDir.toFile();
		try {
			dir.mkdirs();
			// TODO: missing notes
			Arrays.asList("contact", "calendar", "task").forEach(data -> exportData(outputDir, domain, de, data));

			ctx.info("Creating archive file, can take a moment...");
			File archiveFile = createArchive(outputDir, de);
			ctx.info("Archive file for " + de.value.email + " created as : " + archiveFile.getAbsolutePath());
		} finally {
			FileUtils.delete(dir);
		}

	}

	private File createArchive(Path outputDir, ItemValue<DirEntry> de) {
		Path archiveFile = exportDirectory.resolve(de.value.email + ".tgz");

		try (OutputStream fOut = Files.newOutputStream(archiveFile);
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
		return archiveFile.toFile();
	}

	private void addFileToTarGz(TarArchiveOutputStream tOut, Path path, String base) throws IOException {
		File f = path.toFile();
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
					addFileToTarGz(tOut, child.toPath(), base + "/" + child.getName());
				}
			}
		}
	}

	private void exportData(Path outputDir, ItemValue<Domain> domain, ItemValue<DirEntry> de, String dataType) {
		File outputDataDir = prepareTmpDir(outputDir, dataType);
		try {
			switch (dataType) {
			case "calendar":
				ExportCalendarCommand calendarExportCommand = new ExportCalendarCommand();
				calendarExportCommand.forTarget(de.value.email);
				calendarExportCommand.rootDir = outputDataDir.getAbsolutePath();
				calendarExportCommand.forContext(ctx);
				calendarExportCommand.run();
				break;
			case "contact":
				ExportAddressBookCommand abExportCommand = new ExportAddressBookCommand();
				abExportCommand.forTarget(de.value.email);
				abExportCommand.rootDir = outputDataDir.getAbsolutePath();
				abExportCommand.forContext(ctx);
				abExportCommand.run();
				break;
			case "task":
				ExportTodolistCommand todoExportCommand = new ExportTodolistCommand();
				todoExportCommand.forTarget(de.value.email);
				todoExportCommand.rootDir = outputDataDir.getAbsolutePath();
				todoExportCommand.forContext(ctx);
				todoExportCommand.run();
				break;
//			case "notes":
//				GenericStream.streamToFile(ctx.adminApi().instance(INotes.class, INoteUids.TYPE).exportAll(),
//						outputDataDir);
//				break;
			case "email":
				SdsDataProtectSpool backupSpool = null;
				Map<String, ISdsSyncStore> sdsStores = new HashMap<>();
				if (downloadEmailContent) {
					backupSpool = new SdsDataProtectSpool(outputDataDir.toPath());
					SystemConf config = ctx.adminApi().instance(ISystemConfiguration.class).getValues();
					for (ItemValue<Server> server : Topology.get().all(TagDescriptor.mail_imap.getTag())) {
						Optional<ISdsSyncStore> sdsSyncStore = new SdsStoreLoader().forSysconf(config, server.uid);
						sdsSyncStore.ifPresent(store -> sdsStores.put(server.uid, store));
					}
				}
				MailSdsBackup mailbackup = new MailSdsBackup(outputDataDir.toPath(), sdsStores, backupSpool);
				mailbackup.backupMailbox(domain, de);
				break;
			}

		} catch (Exception e) {
			throw new CliException("Error when exporting " + dataType, e);
		}
	}

	private File prepareTmpDir(Path outputDir, String dataType) {
		File dir = outputDir.resolve(dataType).toFile();
		dir.mkdir();
		return dir;
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}