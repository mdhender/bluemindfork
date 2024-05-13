/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.retry.support.rocks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.rocksdb.BlockBasedTableConfig;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.DBOptions;
import org.rocksdb.LRUCache;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WALRecoveryMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;

public class RocksQueue {

	private static final Logger logger = LoggerFactory.getLogger(RocksQueue.class);

	@SuppressWarnings("serial")
	private static class QueueException extends RuntimeException {

		public QueueException(Throwable e) {
			super(e);
		}

	}

	@FunctionalInterface
	public interface Appender {
		default void write(String txt) {
			try {
				writeImpl(txt);
			} catch (RocksDBException e) {
				throw new QueueException(e);
			}
		}

		void writeImpl(String txt) throws RocksDBException;
	}

	public static record TailRecord(byte[] tailerKey, byte[] indexIfCommitted, String payload) { // NOSONAR

	}

	public interface Tailer {
		default TailRecord next() {
			try {
				return nextImpl();
			} catch (RocksDBException e) {
				throw new QueueException(e);
			}
		}

		default void commit() {
			try {
				commitImpl();
			} catch (RocksDBException e) {
				throw new QueueException(e);
			}
		}

		void commitImpl() throws RocksDBException;

		TailRecord nextImpl() throws RocksDBException;
	}

	private static final byte[] bytes(String s) {
		return s.getBytes();
	}

	private static final byte[] bytes(long l) {
		var ret = new byte[8];
		var buf = Unpooled.wrappedBuffer(ret).writerIndex(0).readerIndex(0);
		buf.writeLong(l);
		return ret;
	}

	private static final long longOf(byte[] b) {
		return Unpooled.wrappedBuffer(b).readLong();
	}

	private static final byte[] END_IDX = bytes("tail_index");

	private RocksDB db;
	private ColumnFamilyHandle indexFamily;
	private ColumnFamilyHandle dataFamily;
	private ColumnFamilyHandle tailFamily;

	private AtomicLong endIndex = new AtomicLong();

	public RocksQueue(String topic) {
		Path root = Paths.get("/var/cache/bm-core/retry-rocks-" + topic);

		try {
			Files.createDirectories(root);
			openDb(root, topic);
			byte[] endIdx = db.get(indexFamily, END_IDX);
			if (endIdx == null) {
				db.put(indexFamily, END_IDX, bytes(0L));
				endIndex = new AtomicLong(0L);
			} else {
				endIndex = new AtomicLong(longOf(endIdx));
			}
		} catch (Exception e) {
			throw new QueueException(e);
		}

	}

	private void openDb(Path srcPath, String topic) throws RocksDBException {
		Options opts = new Options();
		BlockBasedTableConfig blockCfg = new BlockBasedTableConfig();
		blockCfg.setBlockCache(new LRUCache(1L * 1024 * 1024));
		opts.setTableFormatConfig(blockCfg);
		opts.setWalRecoveryMode(WALRecoveryMode.TolerateCorruptedTailRecords);
		opts.setCreateIfMissing(true);
		opts.setCreateMissingColumnFamilies(true);

		List<ColumnFamilyDescriptor> descs = List.of(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY),
				new ColumnFamilyDescriptor(bytes("tail")), new ColumnFamilyDescriptor(bytes("indx")),
				new ColumnFamilyDescriptor(bytes("data")));
		DBOptions dbOpts = new DBOptions(opts);
		List<ColumnFamilyHandle> families = new ArrayList<>();
		this.db = RocksDB.open(dbOpts, srcPath.toString(), descs, families);
		Map<String, ColumnFamilyHandle> indexed = families.stream().collect(Collectors.toMap(cf -> {
			try {
				return new String(cf.getName());
			} catch (RocksDBException e) {
				throw new QueueException(e);
			}
		}, cf -> cf));
		this.tailFamily = indexed.get("tail");
		this.indexFamily = indexed.get("indx");
		this.dataFamily = indexed.get("data");
		Thread rdbShutdown = new Thread(() -> {
			try {
				db.syncWal();
				db.closeE();
				logger.info("Clean shutdown of {}", db);
			} catch (RocksDBException e) {
				logger.error(e.getMessage(), e);
			}
		}, "rocks-" + topic + "-shutdown");
		Runtime.getRuntime().addShutdownHook(rdbShutdown);
	}

	public Appender writer() {
		return this::appendText;
	}

	/**
	 * Compact entries older than those read by the given tailer
	 * 
	 * @param tailer
	 */
	public void compact(String tailer) {
		try {
			long idx = tailerIndex(bytes(tailer));
			db.deleteRange(dataFamily, bytes(0), bytes(idx));
			logger.info("Compact range [0 - {}]", idx);
		} catch (RocksDBException e) {
			throw new QueueException(e);
		}

	}

	public Tailer reader(String name) {
		return new Tailer() {

			private TailRecord lastReturned;

			@Override
			public TailRecord nextImpl() throws RocksDBException {
				lastReturned = readFrom(name);
				return lastReturned;
			}

			@Override
			public void commitImpl() throws RocksDBException {
				if (lastReturned != null) {
					commitIndex(lastReturned.tailerKey, lastReturned.indexIfCommitted);
				}

			}

		};
	}

	protected void commitIndex(byte[] tailerKey, byte[] indexIfCommitted) throws RocksDBException {
		db.put(tailFamily, tailerKey, indexIfCommitted);
	}

	private void appendText(String txt) throws RocksDBException {
		byte[] nextKey = bytes(endIndex.getAndIncrement());
		byte[] val = bytes(txt);
		db.put(dataFamily, nextKey, val);
		db.put(indexFamily, END_IDX, nextKey);
	}

	private TailRecord readFrom(String tailer) throws RocksDBException {
		byte[] tailerKey = bytes(tailer);
		long idx = tailerIndex(tailerKey);
		byte[] nextIndex = bytes(idx + 1);
		byte[] nextVal = db.get(dataFamily, nextIndex);
		if (nextVal != null) {
			return new TailRecord(tailerKey, nextIndex, new String(nextVal));
		} else {
			return null;
		}
	}

	private long tailerIndex(byte[] tailerKey) throws RocksDBException {
		long idx = -1L;
		byte[] tailerIndex = db.get(tailFamily, tailerKey);
		if (tailerIndex == null) {
			db.put(tailFamily, tailerKey, bytes(idx));
		} else {
			idx = longOf(tailerIndex);
		}
		return idx;
	}

}
