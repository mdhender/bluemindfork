/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.directory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.MoreObjects;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.external.IExternalDirectory;
import net.bluemind.mailbox.api.Mailbox;

public class LiveStreamAdapter implements IExternalDirectory {

	private final ILiveStream stream;
	private final ExpiringMemoizingSupplier<DirContent> supplier;
	private static final Logger logger = LoggerFactory.getLogger(LiveStreamAdapter.class);

	private static class FullDirEntry {
		public DirEntry entry;
		public Mailbox mailbox;

		@Override
		public String toString() {
			return MoreObjects.toStringHelper("DE").add("entry", entry).toString();
		}
	}

	private static final ValueReader<ItemValue<FullDirEntry>> fullEntryReader = JsonUtils
			.reader(new TypeReference<ItemValue<FullDirEntry>>() {
			});
	private static final ValueReader<ItemValue<DirEntry>> rawEntryReader = JsonUtils
			.reader(new TypeReference<ItemValue<DirEntry>>() {
			});

	private static class DirContent {
		public final Map<String, ItemValue<DirEntry>> dirEntriesByUid = new HashMap<>();
		public final Map<String, ItemValue<Mailbox>> mboxByName = new HashMap<>();
	}

	private static class StreamSupplier extends ExpiringMemoizingSupplier<DirContent> {
		private final ILiveStream stream;

		public StreamSupplier(ILiveStream ls) {
			this.stream = ls;
		}

		private static final String name(ItemValue<Mailbox> maybeNull) {
			return Optional.ofNullable(maybeNull).map(m -> m.value.name).orElse(null);
		}

		@Override
		public DirContent load() {
			long time = System.currentTimeMillis();
			DirContent dc = new DirContent();
			stream.subscribeAll(null, dataElem -> {
				if (dataElem.payload.length > 0) {
					JsonObject js = new JsonObject(Buffer.buffer(dataElem.payload));
					ItemValue<DirEntry> extracted = null;
					ItemValue<Mailbox> mbox = null;
					if (js.getJsonObject("value").containsKey("entry")) {
						ItemValue<FullDirEntry> read = fullEntryReader.read(new String(dataElem.payload));
						extracted = ItemValue.create(read, read.value.entry);
						if (read.value.mailbox != null) {
							mbox = ItemValue.create(read, read.value.mailbox);
						}
					} else {
						ItemValue<DirEntry> read = rawEntryReader.read(new String(dataElem.payload));
						extracted = read;
					}
					dc.dirEntriesByUid.put(extracted.uid, extracted);
					if (mbox != null) {
						dc.mboxByName.put(mbox.value.name, mbox);
					}
					logger.info("LiveStreamAdapter {} '{}' {}, loc: {}, box: {}", extracted.value.kind,
							extracted.displayName, extracted.uid, extracted.value.dataLocation, name(mbox));
				}
			});
			time = System.currentTimeMillis() - time;
			logger.info("loaded dir in {}ms", time);

			return dc;
		}
	}

	public LiveStreamAdapter(ILiveStream ls) {
		this.stream = ls;
		this.supplier = new StreamSupplier(stream);
	}

	@Override
	public ItemValue<DirEntry> findByEntryUid(String uid) {
		ItemValue<DirEntry> ret = supplier.get().dirEntriesByUid.get(uid);
		logger.info("lookup {} => {}", uid, ret);
		return ret;
	}

	@Override
	public boolean manages(String domainUid) {
		return stream.domainUid().equals(domainUid);
	}

	@Override
	public ItemValue<Mailbox> findByName(String name) {
		ItemValue<Mailbox> ret = supplier.get().mboxByName.get(name);
		logger.info("lookupMbox {} => {}", name, ret);
		return ret;
	}

}
