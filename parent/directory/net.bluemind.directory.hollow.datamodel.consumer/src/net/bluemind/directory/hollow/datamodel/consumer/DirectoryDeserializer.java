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
package net.bluemind.directory.hollow.datamodel.consumer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.HollowConsumer.ObjectLongevityConfig;
import com.netflix.hollow.api.consumer.HollowConsumer.ObjectLongevityDetector;
import com.netflix.hollow.api.consumer.index.UniqueKeyIndex;
import com.netflix.hollow.core.index.HollowHashIndex;
import com.netflix.hollow.core.index.HollowHashIndexResult;
import com.netflix.hollow.core.read.iterator.HollowOrdinalIterator;
import com.netflix.hollow.tools.query.HollowFieldMatchQuery;

import net.bluemind.directory.hollow.datamodel.consumer.Query.QueryType;
import net.bluemind.directory.hollow.datamodel.consumer.internal.LoggingRefreshListener;
import net.bluemind.serialization.client.HollowContext;

public class DirectoryDeserializer {

	/**
	 * system property used to override the filesystem folder holding hollow
	 * directory data.
	 */
	public static final String BASE_DIR_PROP = "hollow.serdes.folder.directory";

	private static final Logger logger = LoggerFactory.getLogger(DirectoryDeserializer.class);
	protected UniqueKeyIndex<AddressBookRecord, String> uidIndex;
	protected UniqueKeyIndex<AddressBookRecord, String> distinguishedNameIndex;
	protected UniqueKeyIndex<AddressBookRecord, Long> minimalIndex;
	protected HollowHashIndex kindIndex;
	protected final HollowConsumer consumer;
	public final HollowContext context;
	private final HollowHashIndex anrIndex;
	private final HollowHashIndex emailIndex;

	private UniqueKeyIndex<OfflineAddressBook, String> rootByDomainUidIndex;

	private final String domainUid;

	private static final Set<String> complexQueryKeys = new HashSet<>(Arrays.asList("anr", "office", "emails"));

	public static final String baseDataDir() {
		return System.getProperty(BASE_DIR_PROP, "/var/spool/bm-hollowed/directory");
	}

	public DirectoryDeserializer(String domain) {
		this(new File(baseDataDir(), domain));
	}

	private static class LongevityConfig implements ObjectLongevityConfig {

		@Override
		public boolean enableLongLivedObjectSupport() {
			return true;
		}

		@Override
		public boolean enableExpiredUsageStackTraces() {
			return false;
		}

		@Override
		public long gracePeriodMillis() {
			return 5000;
		}

		@Override
		public long usageDetectionPeriodMillis() {
			return 60000;
		}

		@Override
		public boolean dropDataAutomatically() {
			return true;
		}

		@Override
		public boolean forceDropData() {
			return false;
		}

	}

	private static class LongevityDetector implements ObjectLongevityDetector {

		@Override
		public void staleReferenceExistenceDetected(int count) {
			if (count > 0) {
				logger.warn("staleReferenceExistenceDetected({})", count);
			}
		}

		@Override
		public void staleReferenceUsageDetected(int count) {
			if (count > 0) {
				logger.warn("staleReferenceUsageDetected({})", count);
			}
		}

	}

	/**
	 * Caching of hollow objects is forbidden
	 */
	private static final ObjectLongevityConfig longevity = new LongevityConfig();
	private static final ObjectLongevityDetector detector = new LongevityDetector();

	public DirectoryDeserializer(File dir) {
		this(dir, true);
	}

	public DirectoryDeserializer(File dir, boolean watchChanges) {
		this.domainUid = dir.getName();
		logger.info("Consuming from directory {} for domain {}", dir.getAbsolutePath(), domainUid);
		this.context = HollowContext.get(dir, "directory", watchChanges);
		this.consumer = new HollowConsumer.Builder<>()//
				.withBlobRetriever(context.blobRetriever).withAnnouncementWatcher(context.announcementWatcher)//
				.withObjectLongevityConfig(longevity).withObjectLongevityDetector(detector)//
				.withGeneratedAPIClass(OfflineDirectoryAPI.class).build();
		this.consumer.addRefreshListener(new LoggingRefreshListener(
				dir.getName() + " ctx:" + context.toString().replace("net.bluemind.serialization.client.", "")));

		this.consumer.triggerRefresh();
		logger.info("Current version: {}", consumer.getCurrentVersionId());

		this.rootByDomainUidIndex = OfflineAddressBook.uniqueIndex(consumer);
		this.consumer.addRefreshListener(rootByDomainUidIndex);

		this.minimalIndex = UniqueKeyIndex.from(consumer, AddressBookRecord.class).usingPath("minimalid", Long.class);
		this.consumer.addRefreshListener(minimalIndex);
		this.distinguishedNameIndex = UniqueKeyIndex.from(consumer, AddressBookRecord.class)
				.usingPath("distinguishedName", String.class);
		this.consumer.addRefreshListener(distinguishedNameIndex);
		this.uidIndex = UniqueKeyIndex.from(consumer, AddressBookRecord.class).usingPath("uid", String.class);
		this.consumer.addRefreshListener(uidIndex);
		this.anrIndex = new HollowHashIndex(consumer.getStateEngine(), "AddressBookRecord", "", "anr.element.token");
		anrIndex.listenForDeltaUpdates();
		this.emailIndex = new HollowHashIndex(consumer.getStateEngine(), "AddressBookRecord", "",
				"emails.element.ngrams.element.value");
		emailIndex.listenForDeltaUpdates();
		this.kindIndex = new HollowHashIndex(consumer.getStateEngine(), "AddressBookRecord", "", "kind.value");
		kindIndex.listenForDeltaUpdates();
	}

	public boolean isWatcherListening() {
		return context.isWatcherListening();
	}

	public Collection<AddressBookRecord> all() {
		OfflineDirectoryAPI api = (OfflineDirectoryAPI) consumer.getAPI();
		return api.getAllAddressBookRecord();
	}

	public Optional<OfflineAddressBook> root() {
		return Optional.ofNullable(rootByDomainUidIndex.findMatch(domainUid));
	}

	public List<AddressBookRecord> search(List<Predicate<? super AddressBookRecord>> predicates) {
		Stream<AddressBookRecord> stream = StreamSupport.stream(all().spliterator(), false);
		for (Predicate<? super AddressBookRecord> predicate : predicates) {
			stream = stream.filter(predicate);
		}
		return stream.collect(Collectors.toList());
	}

	public Optional<AddressBookRecord> byDistinguishedName(String distinguishedName) {
		return Optional.ofNullable(distinguishedNameIndex.findMatch(distinguishedName.toLowerCase()));
	}

	public Optional<AddressBookRecord> byUid(String uid) {
		return Optional.ofNullable(uidIndex.findMatch(uid));
	}

	public Optional<AddressBookRecord> byMinimalId(long minimalId) {
		return Optional.ofNullable(minimalIndex.findMatch(minimalId));
	}

	public List<AddressBookRecord> byNameOrEmailPrefix(String value) {
		return byHash(anrIndex, value);
	}

	public Optional<AddressBookRecord> byEmail(String email) {
		return byEmailPrefix(email).stream().findFirst();
	}

	private List<AddressBookRecord> byEmailPrefix(String email) {
		return byHash(emailIndex, email);
	}

	public List<AddressBookRecord> byKind(String kind) {
		return byHash(kindIndex, kind);
	}

	private List<AddressBookRecord> byHash(HollowHashIndex hash, String kind) {
		HollowHashIndexResult findMatches = hash.findMatches(kind);
		if (findMatches == null) {
			return Collections.emptyList();
		}
		OfflineDirectoryAPI api = (OfflineDirectoryAPI) consumer.getAPI();
		List<AddressBookRecord> results = new ArrayList<>(findMatches.numResults());
		HollowOrdinalIterator it = findMatches.iterator();
		int ordinal = it.next();
		while (ordinal != HollowOrdinalIterator.NO_MORE_ORDINALS) {
			results.add(api.getAddressBookRecord(ordinal));
			ordinal = it.next();
		}
		return results;
	}

	public SearchResults byKind(List<String> kinds, int offset, int limit, Predicate<AddressBookRecord> filter) {
		List<AddressBookRecord> all = kinds.stream() //
				.flatMap(kind -> byKind(kind).stream()) //
				.filter(filter) //
				.sorted((a, b) -> a.getName().compareTo(b.getName())) //
				.collect(Collectors.toList());
		int total = all.size();
		if (offset < 0) {
			offset = 0;
		}
		if (limit < 0) {
			limit = total;
		}
		offset = Math.min(total, offset);
		int to = Math.min(total, offset + limit);
		return new SearchResults(total, all.subList(offset, to));
	}

	public List<AddressBookRecord> search(Query query) {
		if (query.type == QueryType.VALUE && !complexQueryKeys.contains(query.key)) {
			return simpleQuery(query.key, query.value);
		} else {
			return complexQuery(query);
		}
	}

	private List<AddressBookRecord> complexQuery(Query query) {
		return all().stream().filter(toFilter(query)).collect(Collectors.toList());
	}

	private Predicate<? super AddressBookRecord> toFilter(Query query) {
		return entry -> eval(query, entry);
	}

	private boolean eval(Query query, AddressBookRecord entry) {
		switch (query.type) {
		case VALUE:
			return evalValue(query.key, query.value, entry);
		case AND:
			boolean match = true;
			for (Query child : query.children) {
				match = match && eval(child, entry);
			}
			return match;
		case OR:
			match = false;
			for (Query child : query.children) {
				match = match || eval(child, entry);
			}
			return match;
		default:
			return false;
		}
	}

	private boolean evalValue(String key, String value, AddressBookRecord entry) {
		return AddressBookMatcher.matches(key, value, root(), entry);
	}

	private List<AddressBookRecord> simpleQuery(String key, String value) {
		HollowFieldMatchQuery query = new HollowFieldMatchQuery(consumer.getStateEngine());
		Map<String, BitSet> selection = query.findMatchingRecords(key, value);
		if (!selection.containsKey("AddressBookRecord")) {
			return Collections.emptyList();
		}

		BitSet results = selection.get("AddressBookRecord");
		List<AddressBookRecord> ret = new ArrayList<>();
		int index = 0;
		int next = results.nextSetBit(index);
		while (next != -1) {
			OfflineDirectoryAPI api = (OfflineDirectoryAPI) consumer.getAPI();
			ret.add(api.getAddressBookRecord(next));
			index = next + 1;
			next = results.nextSetBit(index);
		}
		return ret;
	}

}
