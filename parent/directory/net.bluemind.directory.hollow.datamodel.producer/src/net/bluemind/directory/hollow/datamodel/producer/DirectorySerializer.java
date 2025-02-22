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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.directory.hollow.datamodel.producer;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;
import com.netflix.hollow.api.consumer.HollowConsumer.BlobRetriever;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemAnnouncementWatcher;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemBlobRetriever;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.BlobStorageCleaner;
import com.netflix.hollow.api.producer.HollowProducer.Incremental;
import com.netflix.hollow.api.producer.fs.HollowFilesystemAnnouncer;
import com.netflix.hollow.api.producer.fs.HollowFilesystemPublisher;
import com.netflix.hollow.core.write.objectmapper.RecordPrimaryKey;

import net.bluemind.common.hollow.BmFilesystemBlobStorageCleaner;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ContainerChangeset;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.serialization.DataSerializer;
import net.bluemind.core.serialization.HzHollowAnnouncer;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.hollow.datamodel.AddressBookRecord;
import net.bluemind.directory.hollow.datamodel.Cert;
import net.bluemind.directory.hollow.datamodel.DataLocation;
import net.bluemind.directory.hollow.datamodel.OfflineAddressBook;
import net.bluemind.directory.hollow.datamodel.consumer.DirectoryVersionReader;
import net.bluemind.directory.hollow.datamodel.producer.EdgeNgram.EmailEdgeNGram;
import net.bluemind.directory.hollow.datamodel.producer.impl.DomainVersions;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class DirectorySerializer implements DataSerializer {

	private HollowConsumer.BlobRetriever blobRetriever;

	private HollowConsumer.AnnouncementWatcher announcementWatcher;
	private static final String BASE_DATA_DIR = "/var/spool/bm-hollowed/directory";

	private static final Logger logger = LoggerFactory.getLogger(DirectorySerializer.class);

	private ServerSideServiceProvider prov;
	private Incremental producer;
	private final String domainUid;
	private final Object produceLock;

	private static final boolean DROP_HIDDEN = new File("/etc/bm/hollow.no.hidden").exists();

	public DirectorySerializer(String domainUid) {
		this.domainUid = domainUid;
		this.produceLock = new Object();
		initOrReset();
	}

	public void start() {
		if (!restoreIfAvailable(producer, blobRetriever, announcementWatcher)) {
			produce();
		}
	}

	private void initOrReset() {
		try {
			init();
		} catch (HollowCorruptedException e) {
			logger.warn("Trying to recreate from scratch, cause: {}", e.getMessage());
			remove();
			init();
		}
	}

	public void init() {
		this.prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		File localPublishDir = createDataDir();

		HollowFilesystemPublisher publisher = new HollowFilesystemPublisher(localPublishDir.toPath());
		HollowFilesystemAnnouncer announcer = new HzHollowAnnouncer("directory/" + domainUid, localPublishDir);
		try {
			this.announcementWatcher = new HollowFilesystemAnnouncementWatcher(localPublishDir.toPath());
		} catch (NumberFormatException nfe) {
			throw new HollowCorruptedException("Corrupted hollow directory, invalid announced.version format", nfe);
		}

		BlobStorageCleaner cleaner = new BmFilesystemBlobStorageCleaner(localPublishDir, 10);
		this.producer = HollowProducer.withPublisher(publisher).withAnnouncer(announcer) //
				.noIntegrityCheck().withBlobStorageCleaner(cleaner).buildIncremental();
		producer.initializeDataModel(AddressBookRecord.class);
		producer.initializeDataModel(OfflineAddressBook.class);
		logger.info("Announcement watcher current version: {}", announcementWatcher.getLatestVersion());
		this.blobRetriever = new HollowFilesystemBlobRetriever(localPublishDir.toPath());
	}

	/**
	 * @return the version of the hollow snap
	 */
	public long produce() {
		synchronized (produceLock) {
			return serializeIncrement();
		}
	}

	private File createDataDir() {
		File localPublishDir = getDataDir();
		localPublishDir.mkdirs();
		return localPublishDir;
	}

	public File getDataDir() {
		return new File(BASE_DATA_DIR, domainUid);
	}

	private boolean restoreIfAvailable(Incremental producer, BlobRetriever retriever,
			AnnouncementWatcher unpinnableAnnouncementWatcher) {

		long latestVersion = unpinnableAnnouncementWatcher.getLatestVersion();
		try {
			if (latestVersion != AnnouncementWatcher.NO_ANNOUNCEMENT_AVAILABLE) {
				producer.restore(latestVersion, retriever);
				return true;
			}
			return false;
		} catch (Exception e) {
			logger.error("Could not restore existing hollow snapshot for {}", domainUid, e);
			return false;
		}
	}

	private long serializeIncrement() {
		Map<String, OfflineAddressBook> oabs = new HashMap<>();

		IDomains domApi = prov.instance(IDomains.class);
		IDirectory dirApi = prov.instance(IDirectory.class, domainUid);
		IMailboxes mboxApi = prov.instance(IMailboxes.class, domainUid);
		ItemValue<Domain> domain = domApi.get(domainUid);
		String installationId = InstallationId.getIdentifier();

		long version = Optional.ofNullable(DomainVersions.get().getIfPresent(domainUid)).orElse(0L);
		if (StateContext.getState() == SystemState.CORE_STATE_CLONING) {
			return 0l;
		}

		if (version == 0l) {
			DirectoryVersionReader reader = new DirectoryVersionReader(domainUid);
			version = reader.version();
			logger.info("[{}] Version fetched from hollow root is {}", domainUid, version);
		}

		ContainerChangeset<String> changeset = dirApi.changeset(version);
		List<String> allUids = new ArrayList<>(Sets.newHashSet(Iterables.concat(changeset.created, changeset.updated)));
		logger.info("Sync from v{} gave +{} / -{} uid(s)", version, allUids.size(), changeset.deleted.size());
		final Map<String, DataLocation> locationCache = new HashMap<>();

		long hollowVersion = producer.runIncrementalCycle(populator -> {
			OfflineAddressBook oab = oabs.computeIfAbsent(domainUid, d -> createOabEntry(domain, changeset.version));
			oab.sequence = (int) changeset.version;
			populator.addOrModify(oab);
			for (List<String> dirPartition : Lists.partition(allUids, 100)) {
				List<ItemValue<DirEntry>> entries = loadEntries(dirApi, dirPartition);
				List<String> uidWithEmails = entries.stream().filter(iv -> iv.value.email != null).map(iv -> iv.uid)
						.collect(Collectors.toList());

				List<ItemValue<Mailbox>> mailboxes = mboxApi.multipleGet(uidWithEmails);
				for (ItemValue<DirEntry> entry : entries) {
					ItemValue<Mailbox> mailbox = mailboxes.stream().filter(m -> m.uid.equals(entry.value.entryUid))
							.findAny().orElse(null);
					dirEntryToAddressBookRecord(domain, entry, mailbox, locationCache, installationId)
							.ifPresent(rec -> {
								if (dropHiddenEntry(entry)) {
									populator.delete(new RecordPrimaryKey("AddressBookRecord",
											new String[] { entry.value.entryUid }));
								} else {
									populator.addOrModify(rec);
								}

							});
				}
			}
			for (String uidToRm : changeset.deleted) {
				populator.delete(new RecordPrimaryKey("AddressBookRecord", new String[] { uidToRm }));
			}
		});

		logger.info("Created new incremental hollow snap (dir v{}, hollow v{})", changeset.version, hollowVersion);
		DomainVersions.get().put(domainUid, changeset.version);
		return hollowVersion;
	}

	private boolean dropHiddenEntry(ItemValue<DirEntry> de) {
		return DROP_HIDDEN && de.value.hidden;
	}

	private List<ItemValue<DirEntry>> loadEntries(IDirectory dirApi, List<String> dirPartition) {
		List<ItemValue<DirEntry>> entries;
		try {
			entries = dirApi.getMultiple(dirPartition);
		} catch (ServerFault e) {
			entries = new ArrayList<>();
			for (String uid : dirPartition) {
				try {
					entries.add(dirApi.getMultiple(Arrays.asList(uid)).get(0));
				} catch (ServerFault e1) {
					logger.warn("Skipping broken item {}", uid, e1);
				}
			}
		}

		return entries.stream().filter(this::supportedType).collect(Collectors.toList());
	}

	private boolean supportedType(ItemValue<DirEntry> iv) {
		return !iv.value.system
				&& (iv.value.kind == Kind.USER || iv.value.kind == Kind.GROUP || iv.value.kind == Kind.MAILSHARE
						|| iv.value.kind == Kind.RESOURCE || iv.value.kind == Kind.EXTERNALUSER
						|| iv.value.kind == Kind.ADDRESSBOOK || iv.value.kind == Kind.CALENDAR);
	}

	private Optional<AddressBookRecord> dirEntryToAddressBookRecord(ItemValue<Domain> domain, ItemValue<DirEntry> entry,
			ItemValue<Mailbox> box, Map<String, DataLocation> datalocationCache, String installationId) {
		AddressBookRecord rec = new AddressBookRecord();
		Optional<AddressBookRecord> optRec = DirEntrySerializer.get(domain.uid, entry).map(
				serializer -> prepareRecord(domain, entry, box, datalocationCache, installationId, rec, serializer));
		if (!optRec.isPresent()) {
			logger.warn("Integrity problem on entry {}", entry);
		}
		return optRec;
	}

	@SuppressWarnings("unchecked")
	private AddressBookRecord prepareRecord(ItemValue<Domain> domain, ItemValue<DirEntry> entry, ItemValue<Mailbox> box,
			Map<String, DataLocation> datalocationCache, String installationId, AddressBookRecord rec,
			DirEntrySerializer serializer) {
		rec.uid = entry.uid;
		rec.distinguishedName = entryDN(entry.value.kind, rec.uid, domain.uid, installationId);
		rec.minimalid = entry.internalId;
		rec.created = serializer.get(DirEntrySerializer.Property.Created).toDate();
		rec.updated = serializer.get(DirEntrySerializer.Property.Updated).toDate();
		rec.domain = domain.uid;
		String server = entry.value.dataLocation;
		if (server != null) {
			rec.dataLocation = datalocationCache.computeIfAbsent(server, s -> {
				@SuppressWarnings("unchecked")
				List<String> list = (List<String>) serializer.get(DirEntrySerializer.Property.DataLocation).toList();
				DataLocation location = null;
				if (!list.isEmpty()) {
					location = new DataLocation(list.get(0), list.get(1));
				}
				return location;
			});
		}
		Set<String> aliases = new HashSet<>();
		aliases.add(domain.uid);
		aliases.addAll(domain.value.aliases);
		rec.emails = entry.value.kind != Kind.EXTERNALUSER ? toEmails(box, aliases) : toEmails(entry.value.email);
		rec.name = serializer.get(DirEntrySerializer.Property.DisplayName).toString();
		rec.email = serializer.get(DirEntrySerializer.Property.Email).toString();
		rec.kind = serializer.get(DirEntrySerializer.Property.Kind).toString();
		rec.surname = serializer.get(DirEntrySerializer.Property.Surname).toString();
		rec.givenName = serializer.get(DirEntrySerializer.Property.GivenName).toString();
		rec.title = serializer.get(DirEntrySerializer.Property.Title).toString();
		rec.officeLocation = null;
		rec.departmentName = serializer.get(DirEntrySerializer.Property.DepartmentName).toString();
		rec.companyName = serializer.get(DirEntrySerializer.Property.CompanyName).toString();
		rec.assistant = serializer.get(DirEntrySerializer.Property.Assistant).toString();
		rec.addressBookManagerDistinguishedName = serializer
				.get(DirEntrySerializer.Property.AddressBookManagerDistinguishedName).toString();
		rec.addressBookPhoneticGivenName = serializer.get(DirEntrySerializer.Property.GivenName).toString();
		rec.addressBookPhoneticSurname = serializer.get(DirEntrySerializer.Property.Surname).toString();
		rec.addressBookPhoneticCompanyName = serializer.get(DirEntrySerializer.Property.CompanyName).toString();
		rec.addressBookPhoneticDepartmentName = serializer.get(DirEntrySerializer.Property.DepartmentName).toString();
		rec.streetAddress = serializer.get(DirEntrySerializer.Property.StreetAddress).toString();
		rec.postOfficeBox = serializer.get(DirEntrySerializer.Property.postOfficeBox).toString();
		rec.locality = serializer.get(DirEntrySerializer.Property.Locality).toString();
		rec.stateOrProvince = serializer.get(DirEntrySerializer.Property.StateOrProvince).toString();
		rec.postalCode = serializer.get(DirEntrySerializer.Property.PostalCode).toString();
		rec.country = serializer.get(DirEntrySerializer.Property.Country).toString();
		rec.businessTelephoneNumber = serializer.get(DirEntrySerializer.Property.BusinessTelephoneNumber).toString();
		rec.homeTelephoneNumber = serializer.get(DirEntrySerializer.Property.HomeTelephoneNumber).toString();
		rec.business2TelephoneNumbers = serializer.get(DirEntrySerializer.Property.Business2TelephoneNumbers)
				.toString();
		rec.mobileTelephoneNumber = serializer.get(DirEntrySerializer.Property.MobileTelephoneNumber).toString();
		rec.pagerTelephoneNumber = serializer.get(DirEntrySerializer.Property.PagerTelephoneNumber).toString();
		rec.primaryFaxNumber = serializer.get(DirEntrySerializer.Property.PrimaryFaxNumber).toString();
		rec.assistantTelephoneNumber = serializer.get(DirEntrySerializer.Property.AssistantTelephoneNumber).toString();
		rec.userCertificate = null;
		List<Value> certs = (List<Value>) serializer.get(DirEntrySerializer.Property.AddressBookX509Certificate)
				.toList();
		rec.addressBookX509Certificate = certs.stream().map(val -> new Cert(val.toByteArray())).toList();
		List<Value> certsDer = (List<Value>) serializer.get(DirEntrySerializer.Property.UserX509Certificate).toList();
		rec.userX509Certificate = certsDer.stream().map(val -> new Cert(val.toByteArray())).toList();
		rec.thumbnail = serializer.get(DirEntrySerializer.Property.ThumbnailPhoto).toByteArray();
		rec.hidden = serializer.get(DirEntrySerializer.Property.Hidden).toBoolean();
		rec.anr = new AnrTokens().compute(rec);
		return rec;
	}

	private List<net.bluemind.directory.hollow.datamodel.Email> toEmails(ItemValue<Mailbox> box,
			Set<String> domainAliases) {
		if (box == null || box.value == null || box.value.emails == null) {
			return Collections.emptyList();
		}

		return box.value.emails.stream().flatMap(e -> {
			if (!e.allAliases) {
				return Stream.of(new net.bluemind.directory.hollow.datamodel.Email(e.address,
						new EmailEdgeNGram().compute(e.address), e.isDefault, e.allAliases));
			} else {
				String left = e.address.split("@")[0];
				return domainAliases.stream().map(d -> {
					String value = left + "@" + d;
					return new net.bluemind.directory.hollow.datamodel.Email(value, new EmailEdgeNGram().compute(value),
							e.isDefault, e.allAliases);
				});
			}
		}).collect(Collectors.toList());
	}

	private List<net.bluemind.directory.hollow.datamodel.Email> toEmails(String address) {
		return Arrays.asList(new net.bluemind.directory.hollow.datamodel.Email(address,
				new EmailEdgeNGram().compute(address), true, false));
	}

	private OfflineAddressBook createOabEntry(ItemValue<Domain> domain, long version) {
		OfflineAddressBook oab = new OfflineAddressBook();
		oab.containerGuid = UUID.nameUUIDFromBytes((domain.internalId + ":" + domain.uid).getBytes()).toString();
		oab.hierarchicalRootDepartment = null;
		oab.distinguishedName = "/";
		oab.name = "\\Default Global Address List";
		oab.sequence = (int) version;
		oab.domainName = domain.uid;
		oab.domainAliases = domain.value.aliases;
		return oab;
	}

	public void remove() {
		try {
			logger.info("Removing data dir {}", getDataDir());
			deleteDataDir();
		} catch (Exception e) {
			logger.warn("Cannot delete data dir {}", getDataDir());
		}
	}

	public void deleteDataDir() throws IOException {
		Path directory = Paths.get(getDataDir().getAbsolutePath());
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});

	}

	@Override
	public HollowConsumer.BlobRetriever getBlobRetriever() {
		return blobRetriever;
	}

	@Override
	public long getLastVersion() {
		return announcementWatcher.getLatestVersion();
	}

	public static String entryDN(BaseDirEntry.Kind kind, String entryUid, String domainUid, String installationId) {
		return String.format("%s/ou=%s/cn=Recipients/cn=%s:%s", getOrgDn(installationId), domainUid, kind.toString(),
				entryUid).toLowerCase();
	}

	public static String getOrgDn(String installationId) {
		logger.debug("inst id is unused for now {}", installationId);
		return "/o=Mapi";
	}

	@SuppressWarnings("serial")
	private class HollowCorruptedException extends RuntimeException {

		public HollowCorruptedException(String message, Throwable cause) {
			super(message, cause);
		}

	}
}
