package net.bluemind.delivery.rules;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.common.util.concurrent.MoreExecutors;

public class MailboxVacationSendersCache {

	public static class Factory {
		private static Map<String, Factory> INSTANCES = new HashMap<>();

		public static Factory build(String directory) {
			return INSTANCES.computeIfAbsent(directory, (key) -> new Factory(key));
		}

		private final File root;
		private final Cache<String, MailboxVacationSendersCache> mailboxesCache;

		private Factory(String directory) {
			this.root = new File(directory);
			this.root.mkdirs();
			this.mailboxesCache = Caffeine.newBuilder() //
					.executor(MoreExecutors.directExecutor()) //
					.removalListener((String key, MailboxVacationSendersCache cache, RemovalCause cause) -> {
						if (cache != null) {
							cache.clearAndClose();
						}
					}).build();
		}

		public MailboxVacationSendersCache get(String mailboxUid) {
			return mailboxesCache.get(mailboxUid, key -> new MailboxVacationSendersCache(root, mailboxUid));
		}

		public void clear(String mailboxUid) {
			mailboxesCache.invalidate(mailboxUid);
		}
	}

	private final ScheduledExecutorService expireExecutors = Executors.newScheduledThreadPool(3);
	private final long expirationPeriod;
	private final DB db;
	private final HTreeMap<String, Long> cache;

	private MailboxVacationSendersCache(File root, String mailboxUid, long ttl, TimeUnit ttlUnit) {
		File context = new File(root, "vacation_reply_" + mailboxUid + ".mapdb");
		this.db = DBMaker.fileDB(context) //
				.fileMmapEnable() //
				.fileMmapPreclearDisable() //
				.cleanerHackEnable() //
				.checksumHeaderBypass() //
				.transactionEnable() //
				.make();
		this.cache = db.hashMap("senders") //
				.keySerializer(Serializer.STRING_DELTA2) //
				.valueSerializer(Serializer.LONG_DELTA) //
				.expireAfterCreate(ttl, ttlUnit) //
				.expireAfterUpdate(ttl, ttlUnit) //
				.expireExecutor(expireExecutors) //
				.expireExecutorPeriod(1000) //
				.createOrOpen();
		this.expirationPeriod = ttlUnit.toMillis(ttl);
	}

	private MailboxVacationSendersCache(File root, String mailboxUid) {
		this(root, mailboxUid, 3, TimeUnit.DAYS);
	}

	public <T> T ifMissingDoGetOrElseGet(String senderEmail, Supplier<T> doGet, Supplier<T> orElseGet) {
		if (senderEmail == null || contains(senderEmail)) {
			return orElseGet.get();
		}
		T result = doGet.get();
		put(senderEmail);
		return result;
	}

	private boolean contains(String senderEmail) {
		Long timestamp = cache.get(senderEmail);
		// After a restart, we might have some key with a wrong ttl,
		// so we double check here.
		return timestamp != null && (System.currentTimeMillis() - timestamp) < expirationPeriod;
	}

	private void put(String senderEmail) {
		cache.put(senderEmail, System.currentTimeMillis());
		db.commit();
	}

	private void clearAndClose() {
		if (!isClosed()) {
			clear();
			close();
		}
	}

	private boolean isClosed() {
		return db.isClosed();
	}

	private void clear() {
		cache.clear();
		db.commit();
	}

	private void close() {
		cache.close();
		db.close();
	}
}
