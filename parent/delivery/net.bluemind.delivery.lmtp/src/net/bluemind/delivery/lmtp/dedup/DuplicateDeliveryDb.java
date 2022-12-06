/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.delivery.lmtp.dedup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.LongAdder;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.TtlDB;
import org.rocksdb.WALRecoveryMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.delivery.lmtp.common.FreezableDeliveryContent;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.lmtp.config.DeliveryConfig;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class DuplicateDeliveryDb {

	@FunctionalInterface
	public interface UniqueMessageAction {

		void run() throws IOException;

	}

	private static final Logger logger = LoggerFactory.getLogger(DuplicateDeliveryDb.class);

	private final TtlDB ttlDb;
	private final Duration window;
	private final Counter dedupCounter;

	private final LongAdder deduplications;

	private static final byte[] CONST_VALUE = new byte[] { 0x01 };

	private static final DuplicateDeliveryDb INSTANCE = new DuplicateDeliveryDb(
			DeliveryConfig.get().getDuration("lmtp.dedup.window"),
			DeliveryConfig.get().getString("lmtp.dedup.db-path"));

	public static final DuplicateDeliveryDb get() {
		return INSTANCE;
	}

	DuplicateDeliveryDb(Duration dedupWindow, String dbPath) {
		this.window = dedupWindow;
		this.deduplications = new LongAdder();
		Registry reg = MetricsRegistry.get();
		IdFactory idf = new IdFactory("bm-lmtpd", reg, DuplicateDeliveryDb.class);
		this.dedupCounter = reg.counter(idf.name("deduplicated"));

		RocksDB.loadLibrary();
		Path dedupPath = Paths.get(dbPath);
		try {
			Files.createDirectories(dedupPath);
			Options opts = new Options();
			opts.setWalRecoveryMode(WALRecoveryMode.SkipAnyCorruptedRecords);
			opts.setCreateIfMissing(true);
			this.ttlDb = TtlDB.open(opts, dedupPath.toString(), (int) dedupWindow.toSeconds(), false);
			logger.info("{} duplicate delivery protection opened in {}, duration ~ {}h.", ttlDb, dbPath,
					window.toHours());
		} catch (IOException | RocksDBException e) {
			throw new ServerFault("Consider deleting the '" + dbPath
					+ "' directory to start with a fresh duplicate delivery database.", e);
		}
	}

	public long dedupCount() {
		// we can't use the spectator counter in tests as the implementation is a noop
		// when the java agent is not setup
		return deduplications.sum();
	}

	public boolean runIfUnique(FreezableDeliveryContent fc, UniqueMessageAction action) throws IOException {
		return runIfUnique(fc.content().message().getMessageId(), fc.content().box(), action);
	}

	public boolean runIfUnique(String messageId, ResolvedBox target, UniqueMessageAction action) throws IOException {
		JsonObject key = new JsonObject();
		key.put("m", messageId).put("d", target.dom.uid).put("u", target.entry.entryUid);
		String serializedKey = key.encode();

		byte[] keyBytes = serializedKey.getBytes();
		byte[] value = getOrFail(keyBytes);
		if (value == null) {
			try {
				action.run();
				putOrFail(keyBytes);
				return true;
			} catch (IOException ioe) {
				deleteOrFail(keyBytes);
				throw ioe;
			} catch (Exception e) {
				deleteOrFail(keyBytes);
				throw new IOException(e);
			}
		} else {
			logger.warn("Message delivery {} skipped as message id was seen in the last {} day(s)", serializedKey,
					window.toDays());
			dedupCounter.increment();
			deduplications.increment();
			return false;
		}

	}

	private void putOrFail(byte[] keyBytes) {
		try {
			ttlDb.put(keyBytes, CONST_VALUE);
		} catch (RocksDBException e) {
			throw new ServerFault(e);
		}
	}

	private byte[] getOrFail(byte[] keyBytes) {
		try {
			return ttlDb.get(keyBytes);
		} catch (RocksDBException e) {
			throw new ServerFault(e);
		}
	}

	private void deleteOrFail(byte[] keyBytes) {
		try {
			ttlDb.delete(keyBytes);
		} catch (RocksDBException e) {
			throw new ServerFault(e);
		}
	}

	void close() {
		logger.info("Closing {}", ttlDb);
		ttlDb.close();
	}

}
