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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.directory.common.SingleOrDomainOperation;
import net.bluemind.cli.utils.Tasks;
import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.server.api.IServer;
import net.bluemind.todolist.api.ITodoLists;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.todolist.api.IVTodo;
import net.bluemind.user.api.IUser;
import net.bluemind.utils.FileUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "import", description = "import user data from an archive file, existing data will be erased.")
public class UserImportCommand extends SingleOrDomainOperation {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("user");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return UserImportCommand.class;
		}
	}

	private static final int BUFFER_MAX_SIZE = 1024;

	@Option(names = "--archiveFile", required = true, description = "BM user archive path")
	public Path archiveFile = null;

	private String domainUid;

	@Override
	public void synchronousDirOperation(String domainUid, ItemValue<DirEntry> de) throws IOException {
		this.domainUid = domainUid;
		File archive = archiveFile.toFile();
		if (!archive.exists() || archive.isDirectory()) {
			throw new CliException("Invalid archive file");
		}

		Path tempDir = Files.createDirectory(Paths.get("bm-import"));
		try {
			extractArchive(archive.toPath(), tempDir);

			// skip first directory level
			tempDir = Files.list(tempDir).findFirst().get();
			importDatas(de, tempDir);

			IServer serversApi = ctx.adminApi().instance(IServer.class, InstallationId.getIdentifier());

			Tasks.follow(ctx, true,
					serversApi.submit(de.value.dataLocation,
							"bm-cli maintenance repair --ops mailboxFilesystem " + de.value.email),
					"Cannot repair (mailboxFilesystem) " + de.uid);
			Tasks.follow(ctx, true,
					serversApi.submit(de.value.dataLocation,
							"bm-cli maintenance repair --ops mailboxAcls " + de.value.email),
					"Cannot repair (mailboxAcls) " + de.uid);

		} catch (IOException e) {
			ctx.error("Error extracting archive " + e.getMessage());
			throw new CliException(e);
		} finally {
			FileUtils.delete(tempDir.toFile());
		}
	}

	private void extractArchive(Path archivePath, Path tempDir) throws IOException {

		try (InputStream in = Files.newInputStream(archivePath)) {

			GzipCompressorInputStream gzipIn = new GzipCompressorInputStream(in);
			try (TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {
				TarArchiveEntry entry;

				while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
					Path path = Paths.get(tempDir.toString(), entry.getName());

					/** If the entry is a directory, create the directory. **/
					if (entry.isDirectory()) {
						Files.createDirectories(path);
					} else {
						int size = (int) entry.getSize();
						if (size == 0) {
							continue;
						}
						byte data[] = new byte[size];

						try (FileOutputStream fos = new FileOutputStream(path.toString(), false);
								BufferedOutputStream dest = new BufferedOutputStream(fos, size)) {
							int count = 0;
							while ((count = tarIn.read(data, 0, BUFFER_MAX_SIZE)) != -1) {
								dest.write(data, 0, count);
							}
						}
					}
				}
				ctx.info("Archive file extracted successfully to " + tempDir.toString());
			}
		}

	}

	private void importDatas(ItemValue<DirEntry> de, Path tempDir) throws IOException {
		Files.list(tempDir).forEach(s -> {
			try {
				Files.list(s).forEach(subData -> {
					try {
						switch (s.getFileName().toString()) {
						case "contact":
							importContacts(de, subData);
							break;
						case "calendar":
							importCalendars(de, subData);
							break;
						case "task":
							importTasks(de, subData);
							break;
						case "mail":
							importMail(de, subData);
							break;
						default:
							ctx.error("Unknown data directory : " + s);
							break;
						}
					} catch (IOException e) {
						throw new CliException(e);
					}
				});
			} catch (Exception e) {
				throw new CliException("Error importing data", e);
			}
		});
	}

	private void importCalendars(ItemValue<DirEntry> de, Path dir) throws IOException {
		Files.list(dir).forEach(cal -> importCalendar(de, cal));
	}

	private void importCalendar(ItemValue<DirEntry> de, Path icsFile) {
		String calName = decodeName(icsFile);
		ctx.info("Importing calendar : " + calName + " path : " + icsFile.toString());

		// find calendar
		String calUid = null;
		if (calName.equals(de.displayName)) {
			calUid = ICalendarUids.defaultUserCalendar(de.uid);
		} else {
			// create a new one
			ICalendarsMgmt calMgmt = ctx.adminApi().instance(ICalendarsMgmt.class);
			CalendarDescriptor desc = new CalendarDescriptor();
			desc.domainUid = domainUid;
			desc.name = calName;
			desc.owner = de.uid;
			calUid = UUID.randomUUID().toString();
			calMgmt.create(calUid, desc);
		}

		TaskRef ref = ctx.adminApi().instance(IVEvent.class, calUid)
				.importIcs(cliUtils.getStreamFromFile(icsFile.toString()));
		Tasks.follow(ctx, false, ref, String.format("Fail to import calendar for entry %s", de));
	}

	private void importContacts(ItemValue<DirEntry> de, Path dir) throws IOException {
		Files.list(dir).forEach(cal -> importContact(de, cal));
	}

	private void importContact(ItemValue<DirEntry> de, Path vcfFile) {
		String abName = decodeName(vcfFile);
		ctx.info("Importing addressbook : " + abName);

		// find ab
		String abUid = null;
		if (abName.equals("Mes contacts")) {
			abUid = IAddressBookUids.defaultUserAddressbook(de.uid);
		} else if (abName.equals("Contacts collectés")) {
			abUid = IAddressBookUids.collectedContactsUserAddressbook(de.uid);
		} else {
			// create a new one
			IAddressBooksMgmt abMgmt = ctx.adminApi().instance(IAddressBooksMgmt.class, domainUid);
			AddressBookDescriptor desc = new AddressBookDescriptor();
			desc.domainUid = domainUid;
			desc.name = abName;
			desc.owner = de.uid;
			abUid = UUID.randomUUID().toString();
			abMgmt.create(abUid, desc, false);
		}

		try {
			TaskRef ref = ctx.adminApi().instance(IVCardService.class, abUid)
					.importCards(new String(Files.readAllBytes(vcfFile)));
			Tasks.follow(ctx, false, ref, String.format("Fail to import addressbook for entry %s", de));
		} catch (Exception e) {
			throw new CliException("Error importing addressbook " + vcfFile.toString(), e);
		}
	}

	private void importTasks(ItemValue<DirEntry> de, Path dir) throws IOException {
		Files.list(dir).forEach(cal -> importTask(de, cal));
	}

	private void importTask(ItemValue<DirEntry> de, Path vcfFile) {
		String name = decodeName(vcfFile);
		ctx.info("Importing todolist : " + name);

		// find ab
		String uid = null;
		if (name.equals("Mes tâches")) {
			uid = ITodoUids.defaultUserTodoList(de.uid);
		} else {
			// create a new one
			ITodoLists mgmt = ctx.adminApi().instance(ITodoLists.class, domainUid);
			ContainerDescriptor desc = new ContainerDescriptor();
			desc.domainUid = domainUid;
			desc.name = name;
			desc.owner = de.uid;
			uid = UUID.randomUUID().toString();
			mgmt.create(uid, desc);
		}

		try {
			TaskRef ref = ctx.adminApi().instance(IVTodo.class, uid).importIcs(new String(Files.readAllBytes(vcfFile)));
			Tasks.follow(ctx, false, ref, String.format("Fail to import todo for entry %s", de));
		} catch (Exception e) {
			throw new CliException("Error importing todolist " + vcfFile.toString(), e);
		}
	}

	private void importMail(ItemValue<DirEntry> de, Path directory) {
		String type = directory.getFileName().toString();
		String filename = directory.getFileName().toString();

		ctx.info("Importing mail " + type);
		String login = ctx.adminApi().instance(IUser.class, domainUid).getComplete(de.uid).value.login;

		char firstDomainLetter = (Character.isLetter(domainUid.charAt(0))) ? domainUid.charAt(0) : 'q';

		String basePath = filename.equalsIgnoreCase("data") || filename.equalsIgnoreCase("meta")
				? "/var/spool/cyrus/" + filename
				: "/var/spool/bm-hsm/cyrus-archives";
		String cyrusPath = basePath + "/" + de.value.dataLocation + "__" + domainUid.replace('.', '_') + "/domain/"
				+ firstDomainLetter + "/" + domainUid + "/" + firstLetterMailbox(login) + "/user/"
				+ login.replace('.', '^');

		copyEmails(de, directory, cyrusPath);
	}

	private void copyEmails(ItemValue<DirEntry> de, Path directory, String outputDir) {
		String command = String.format("rsync -r %s/ %s", directory.toAbsolutePath().toString(), outputDir);
		IServer serversApi = ctx.adminApi().instance(IServer.class, InstallationId.getIdentifier());
		Tasks.follow(ctx, true, serversApi.submit(de.value.dataLocation, command),
				"Cannot copy mails from " + directory.toAbsolutePath().toString());
	}

	private char firstLetterMailbox(String mbox) {
		Character c = mbox.charAt(0);
		if (Character.isDigit(c)) {
			return 'q';
		} else {
			return c.charValue();
		}
	}

	private String decodeName(Path file) {
		return cliUtils
				.decodeFilename(file.getFileName().toString().substring(0, file.getFileName().toString().length() - 4));
	}

	@Override
	public Kind[] getDirEntryKind() {
		return new Kind[] { Kind.USER };
	}
}