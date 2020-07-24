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
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.netflix.hollow.api.producer.HollowIncrementalProducer;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.BlobStorageCleaner;
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
import net.bluemind.directory.hollow.datamodel.DataLocation;
import net.bluemind.directory.hollow.datamodel.OfflineAddressBook;
import net.bluemind.directory.hollow.datamodel.producer.impl.DomainVersions;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class DirectorySerializer implements DataSerializer {

	private HollowConsumer.BlobRetriever blobRetriever;

	private HollowConsumer.AnnouncementWatcher announcementWatcher;
	private static final String BASE_DATA_DIR = "/var/spool/bm-hollowed/directory";

	private static final Logger logger = LoggerFactory.getLogger(DirectorySerializer.class);

	private ServerSideServiceProvider prov;
	private HollowProducer producer;
	private final String domainUid;
	private final Object produceLock;

	private HollowIncrementalProducer incremental;

	public DirectorySerializer(String domainUid) {
		this.domainUid = domainUid;
		this.produceLock = new Object();
		init();
	}

	public void start() {
		if (!restoreIfAvailable(producer, blobRetriever, announcementWatcher)) {
			produce();
		}
	}

	public void init() {
		this.prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		File localPublishDir = createDataDir();

		HollowFilesystemPublisher publisher = new HollowFilesystemPublisher(localPublishDir);
		HollowFilesystemAnnouncer announcer = new HzHollowAnnouncer("directory/" + domainUid, localPublishDir);
		this.announcementWatcher = new HollowFilesystemAnnouncementWatcher(localPublishDir);

		BlobStorageCleaner cleaner = new BmFilesystemBlobStorageCleaner(localPublishDir, 10);
		this.producer = HollowProducer.withPublisher(publisher).withAnnouncer(announcer) //
				.withBlobStorageCleaner(cleaner).build();
		producer.initializeDataModel(AddressBookRecord.class);
		producer.initializeDataModel(OfflineAddressBook.class);
		logger.info("Announcement watcher current version: {}", announcementWatcher.getLatestVersion());
		this.blobRetriever = new HollowFilesystemBlobRetriever(localPublishDir);
		this.incremental = new HollowIncrementalProducer(producer);
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

	private boolean restoreIfAvailable(HollowProducer producer, BlobRetriever retriever,
			AnnouncementWatcher unpinnableAnnouncementWatcher) {

		long latestVersion = unpinnableAnnouncementWatcher.getLatestVersion();
		if (latestVersion != AnnouncementWatcher.NO_ANNOUNCEMENT_AVAILABLE) {
			producer.restore(latestVersion, retriever);
			return true;
		}
		return false;
	}

	private long serializeIncrement() {
		Map<String, OfflineAddressBook> oabs = new HashMap<>();

		IDomains domApi = prov.instance(IDomains.class);
		IDirectory dirApi = prov.instance(IDirectory.class, domainUid);
		IMailboxes mboxApi = prov.instance(IMailboxes.class, domainUid);
		ItemValue<Domain> domain = domApi.get(domainUid);
		String installationId = InstallationId.getIdentifier();

		long version = Optional.ofNullable(DomainVersions.get().getIfPresent(domainUid)).orElse(0L);

		ContainerChangeset<String> changeset = dirApi.changeset(version);
		List<String> allUids = new ArrayList<>(Sets.newHashSet(Iterables.concat(changeset.created, changeset.updated)));
		logger.info("Sync from v{} gave +{} / -{} uid(s)", version, allUids.size(), changeset.deleted.size());
		Map<String, DataLocation> locationCache = new HashMap<>();
		for (List<String> dirPartition : Lists.partition(allUids, 100)) {
			List<ItemValue<DirEntry>> entries = loadEntries(dirApi, dirPartition);
			List<String> uidWithEmails = entries.stream().filter(iv -> iv.value.email != null).map(iv -> iv.uid)
					.collect(Collectors.toList());

			List<ItemValue<Mailbox>> mailboxes = mboxApi.multipleGet(uidWithEmails);
			for (ItemValue<DirEntry> entry : entries) {
				dirEntryToAddressBookRecord(domain, entry,
						mailboxes.stream().filter(m -> m.uid.equals(entry.value.entryUid)).findAny().orElse(null),
						locationCache, installationId).ifPresent(rec -> {
							rec.addressBook = oabs.computeIfAbsent(domainUid, d -> createOabEntry(domain));
							if (entry.value.archived) {
								incremental.delete(new RecordPrimaryKey("AddressBookRecord",
										new String[] { entry.value.entryUid }));
							} else {
								incremental.addOrModify(rec);
							}

						});
			}
		}
		for (String uidToRm : changeset.deleted) {
			incremental.delete(new RecordPrimaryKey("AddressBookRecord", new String[] { uidToRm }));
		}
		long hollowVersion = incremental.runCycle();
		logger.info("Created new incremental hollow snap (dir v{}, hollow v{})", changeset.version, hollowVersion);
		DomainVersions.get().put(domainUid, changeset.version);
		return hollowVersion;
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
		return !iv.value.system && (iv.value.kind == Kind.USER || iv.value.kind == Kind.GROUP
				|| iv.value.kind == Kind.MAILSHARE || iv.value.kind == Kind.RESOURCE);
	}

	private Optional<AddressBookRecord> dirEntryToAddressBookRecord(ItemValue<Domain> domain, ItemValue<DirEntry> entry,
			ItemValue<Mailbox> box, Map<String, DataLocation> datalocationCache, String installationId) {
		AddressBookRecord rec = new AddressBookRecord();

		String tmpDom = domain.uid;
		Optional<AddressBookRecord> optRec = DirEntrySerializer.get(tmpDom, entry)
				.map(serializer -> prepareRecord(domain, entry, box, datalocationCache, installationId, rec, tmpDom,
						serializer));
		if (!optRec.isPresent()) {
			logger.warn("Integrity problem on entry {}", entry);
		}
		return optRec;
	}

	private AddressBookRecord prepareRecord(ItemValue<Domain> domain, ItemValue<DirEntry> entry, ItemValue<Mailbox> box,
			Map<String, DataLocation> datalocationCache, String installationId, AddressBookRecord rec, String tmpDom,
			DirEntrySerializer serializer) {
		rec.uid = entry.uid;
		rec.distinguishedName = entryDN(entry.value.kind, rec.uid, tmpDom, installationId);
		rec.minimalid = entry.internalId;
		rec.created = serializer.get(DirEntrySerializer.Property.Created).toDate();
		rec.updated = serializer.get(DirEntrySerializer.Property.Updated).toDate();
		rec.domain = tmpDom;
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
		List<String> aliases = new ArrayList<>();
		aliases.add(tmpDom);
		aliases.addAll(domain.value.aliases);
		rec.emails = toEmails(box, aliases);
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
		rec.addressBookX509Certificate = serializer.get(DirEntrySerializer.Property.AddressBookX509Certificate)
				.toByteArray();
		rec.userX509Certificate = serializer.get(DirEntrySerializer.Property.UserX509Certificate).toByteArray();
		rec.thumbnail = serializer.get(DirEntrySerializer.Property.ThumbnailPhoto).toByteArray();
		rec.hidden = serializer.get(DirEntrySerializer.Property.Hidden).toBoolean();
		return rec;
	}

	private List<net.bluemind.directory.hollow.datamodel.Email> toEmails(ItemValue<Mailbox> box,
			List<String> domainAliases) {
		if (box == null || box.value == null || box.value.emails == null) {
			return Collections.emptyList();
		}

		return box.value.emails.stream().flatMap(e -> {
			if (!e.allAliases) {
				return Stream
						.of(net.bluemind.directory.hollow.datamodel.Email.create(e.address, e.isDefault, e.allAliases));
			} else {
				String left = e.address.split("@")[0];
				return domainAliases.stream().map(d -> net.bluemind.directory.hollow.datamodel.Email
						.create(left + "@" + d, e.isDefault, e.allAliases));
			}
		}).collect(Collectors.toList());
	}

	private OfflineAddressBook createOabEntry(ItemValue<Domain> domain) {
		OfflineAddressBook oab = new OfflineAddressBook();
		oab.containerGuid = UUID.nameUUIDFromBytes((domain.internalId + ":" + domain.uid).getBytes()).toString();
		oab.hierarchicalRootDepartment = null;
		oab.distinguishedName = "/";
		oab.name = "\\Default Global Address List";
		oab.sequence = 1;
		oab.domainName = domain.uid;
		oab.domainAliases = domain.value.aliases;
		return oab;
	}

	public HollowProducer getProducer() {
		return producer;
	}

	public void setProducer(HollowProducer producer) {
		this.producer = producer;
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

}
