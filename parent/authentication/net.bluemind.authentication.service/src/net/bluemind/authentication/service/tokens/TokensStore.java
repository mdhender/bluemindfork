/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.authentication.service.tokens;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;
import com.netflix.hollow.api.consumer.HollowConsumer.BlobRetriever;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemAnnouncementWatcher;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemBlobRetriever;
import com.netflix.hollow.api.consumer.index.UniqueKeyIndex;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.BlobStorageCleaner;
import com.netflix.hollow.api.producer.HollowProducer.Builder;
import com.netflix.hollow.api.producer.fs.HollowFilesystemPublisher;
import com.netflix.hollow.core.write.objectmapper.RecordPrimaryKey;

import net.bluemind.authentication.service.Token;
import net.bluemind.common.hollow.BmFilesystemBlobStorageCleaner;

public class TokensStore {
	private static final String BASE_DATA_DIR = "/var/spool/bm-hollowed/tokens";
	private static final Logger logger = LoggerFactory.getLogger(TokensStore.class);

	private static TokensStore instance = new TokensStore();

	public static TokensStore get() {
		return instance;
	}

	private final HollowConsumer consumer;
	private final UniqueKeyIndex<net.bluemind.authentication.service.tokens.Token, String> keyIndex;
	private final HollowProducer.Incremental incremental;

	private TokensStore() {
		File localPublishDir = new File(BASE_DATA_DIR);
		localPublishDir.mkdirs();

		HollowFilesystemPublisher publisher = new HollowFilesystemPublisher(localPublishDir.toPath());

		BlobStorageCleaner cleaner = new BmFilesystemBlobStorageCleaner(localPublishDir, 10);
		Builder<?> builder = HollowProducer.withPublisher(publisher) //
				.withBlobStorageCleaner(cleaner);
		HollowProducer producer = builder.build();
		producer.initializeDataModel(Token.class);
		this.incremental = builder.buildIncremental();

		HollowConsumer.BlobRetriever blobRetriever = new HollowFilesystemBlobRetriever(localPublishDir.toPath());
		if (!restoreIfAvailable(producer, blobRetriever,
				new HollowFilesystemAnnouncementWatcher(localPublishDir.toPath()))) {
			producer.runCycle(state -> state
					.add(new Token(net.bluemind.config.Token.admin0(), "admin0", "global.virt", "core-tok")));
		}

		this.consumer = new HollowConsumer.Builder<>().withBlobRetriever(blobRetriever)
				.withGeneratedAPIClass(TokensAPI.class).build();
		consumer.triggerRefresh();

		this.keyIndex = UniqueKeyIndex.from(consumer, net.bluemind.authentication.service.tokens.Token.class)
				.usingPath("key", String.class);
		consumer.addRefreshListener(this.keyIndex);
	}

	public static void reset() {
		try {
			deleteDataDir();
		} catch (IOException e) {
			logger.warn("Cannot remove token data dir", e);
		}
		instance = new TokensStore();
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

	public synchronized void add(Token tok) {
		long newVersion = incremental.runIncrementalCycle(incrementalState -> incrementalState.addOrModify(tok));
		consumer.triggerRefreshTo(newVersion);
	}

	public synchronized Token remove(String key) {
		Token current = byKey(key);
		if (key != null) {
			long newVersion = incremental.runIncrementalCycle(
					incrementalState -> incrementalState.delete(new RecordPrimaryKey("Token", new String[] { key })));
			consumer.triggerRefreshTo(newVersion);
		}
		return current;
	}

	public Token byKey(String key) {
		net.bluemind.authentication.service.tokens.Token internalTok = keyIndex.findMatch(key);
		if (internalTok != null) {
			return new Token(key, internalTok.getSubjectUid().getValue(), internalTok.getSubjectDomain().getValue(),
					internalTok.getOrigin());
		}
		return null;
	}

	public synchronized int expireOldTokens() {
		TokensAPI api = (TokensAPI) consumer.getAPI();
		Collection<net.bluemind.authentication.service.tokens.Token> tokens = api.getAllToken();
		long now = System.currentTimeMillis();
		List<RecordPrimaryKey> expired = tokens.stream().filter(tok -> now > tok.getExpiresTimestamp())
				.map(net.bluemind.authentication.service.tokens.Token::getKey).filter(Objects::nonNull)
				.map(key -> new RecordPrimaryKey("Token", new String[] { key })).collect(Collectors.toList());
		if (!expired.isEmpty()) {
			long newVersion = incremental.runIncrementalCycle(incrementalState -> {
				for (RecordPrimaryKey expiredToken : expired) {
					incrementalState.delete(expiredToken);
				}
			});
			consumer.triggerRefreshTo(newVersion);
		}
		logger.info("Expired {} token(s), {} remaining.", expired.size(), tokens.size() - expired.size());
		return expired.size();
	}

	private static void deleteDataDir() throws IOException {
		Path directory = Paths.get(BASE_DATA_DIR);
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

}
