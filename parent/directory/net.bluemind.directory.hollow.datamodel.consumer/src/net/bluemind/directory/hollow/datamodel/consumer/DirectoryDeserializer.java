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
import java.util.HashMap;
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
import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;
import com.netflix.hollow.core.index.HollowHashIndex;
import com.netflix.hollow.core.index.HollowHashIndexResult;
import com.netflix.hollow.core.index.HollowPrefixIndex;
import com.netflix.hollow.core.read.iterator.HollowOrdinalIterator;
import com.netflix.hollow.tools.query.HollowFieldMatchQuery;

import net.bluemind.directory.hollow.datamodel.consumer.Query.QueryType;
import net.bluemind.serialization.client.HollowContext;

public class DirectoryDeserializer {

	private static final Logger logger = LoggerFactory.getLogger(DirectoryDeserializer.class);
	private static final String BASE_DATA_DIR = "/var/spool/bm-hollowed/directory";
	protected AddressBookRecordPrimaryKeyIndex uidIndex;
	protected AddressBookRecordPrimaryKeyIndex distinguishedNameIndex;
	protected AddressBookRecordPrimaryKeyIndex minimalIndex;
	protected HollowHashIndex kindIndex;
	protected HollowConsumer consumer;
	private HollowPrefixIndex nameIndex;
	private HollowPrefixIndex emailIndex;
	private static final Set<String> complexQueryKeys = new HashSet<>(Arrays.asList("anr", "office"));

	public DirectoryDeserializer(String domain) {
		this(new File(BASE_DATA_DIR, domain));
	}

	public DirectoryDeserializer(File dir) {
		logger.info("Consuming from directory {}", dir.getAbsolutePath());
		HollowContext context = HollowContext.get(dir, "directory");
		this.consumer = HollowConsumer.withBlobRetriever(context.blobRetriever)
				.withAnnouncementWatcher(watcher(context)).withGeneratedAPIClass(OfflineDirectoryAPI.class).build();

		this.consumer.triggerRefresh();
		logger.info("Current version: {}", consumer.getCurrentVersionId());

		this.minimalIndex = new AddressBookRecordPrimaryKeyIndex(consumer, "minimalid");
		minimalIndex.listenToDataRefresh();
		this.distinguishedNameIndex = new AddressBookRecordPrimaryKeyIndex(consumer, "distinguishedName");
		distinguishedNameIndex.listenToDataRefresh();
		this.uidIndex = new AddressBookRecordPrimaryKeyIndex(consumer, "uid");
		uidIndex.listenToDataRefresh();
		this.nameIndex = new HollowPrefixIndex(consumer.getStateEngine(), "AddressBookRecord", "name.value");
		nameIndex.listenForDeltaUpdates();
		this.emailIndex = new HollowPrefixIndex(consumer.getStateEngine(), "AddressBookRecord",
				"emails.element.address.value");
		emailIndex.listenForDeltaUpdates();
		this.kindIndex = new HollowHashIndex(consumer.getStateEngine(), "AddressBookRecord", "", "kind.value");
		kindIndex.listenForDeltaUpdates();
	}

	protected AnnouncementWatcher watcher(HollowContext ctx) {
		return ctx.announcementWatcher;
	}

	public Collection<AddressBookRecord> all() {
		OfflineDirectoryAPI api = (OfflineDirectoryAPI) consumer.getAPI();
		return api.getAllAddressBookRecord();
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

	public Collection<AddressBookRecord> byNameOrEmailPrefix(String value) {
		Map<Integer, AddressBookRecord> results = new HashMap<>();
		results.putAll(byNamePrefix(value).stream().collect(Collectors.toMap(a -> a.getOrdinal(), a -> a)));
		results.putAll(byEmailPrefix(value).stream().collect(Collectors.toMap(a -> a.getOrdinal(), a -> a)));
		return results.values();
	}

	public Optional<AddressBookRecord> byEmail(String email) {
		return byEmailPrefix(email).stream().findFirst();
	}

	private List<AddressBookRecord> byNamePrefix(String name) {
		return byPrefix(nameIndex, name);
	}

	private List<AddressBookRecord> byEmailPrefix(String email) {
		return byPrefix(emailIndex, email);
	}

	private List<AddressBookRecord> byPrefix(HollowPrefixIndex index, String value) {
		List<AddressBookRecord> results = new ArrayList<>();
		HollowOrdinalIterator it = index.findKeysWithPrefix(value);
		OfflineDirectoryAPI api = (OfflineDirectoryAPI) consumer.getAPI();
		int ordinal = it.next();
		while (ordinal != HollowOrdinalIterator.NO_MORE_ORDINALS) {
			results.add(api.getAddressBookRecord(ordinal));
			ordinal = it.next();
		}
		return results;
	}

	public Collection<AddressBookRecord> byKind(String kind) {
		HollowHashIndexResult findMatches = kindIndex.findMatches(kind);
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

	public SearchResults byKind(List<String> kinds, int offset, int limit, SerializedDirectorySearch search) {
		List<AddressBookRecord> all = new ArrayList<>();
		for (String kind : kinds) {
			all.addAll(search.byKind(kind));
		}
		int total = all.size();
		if (offset < 0) {
			offset = 0;
		}
		if (limit < 0) {
			limit = total;
		}
		offset = Math.min(total, offset);
		int to = Math.min(total, offset + limit);
		return new SearchResults(total, order(all).subList(offset, to));
	}

	private List<AddressBookRecord> order(List<AddressBookRecord> list) {
		return list.stream().sorted((a, b) -> a.getName().getValue().compareTo(b.getName().getValue()))
				.collect(Collectors.toList());
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
		return (record -> eval(query, record));
	}

	private boolean eval(Query query, AddressBookRecord record) {
		switch (query.type) {
		case VALUE:
			return evalValue(query.key, query.value, record);
		case AND:
			boolean match = true;
			for (Query child : query.children) {
				match = match && eval(child, record);
			}
			return match;
		case OR:
			match = false;
			for (Query child : query.children) {
				match = match || eval(child, record);
			}
			return match;
		default:
			return false;
		}
	}

	private boolean evalValue(String key, String value, AddressBookRecord record) {
		return AddressBookMatcher.matches(key, value, record);
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
