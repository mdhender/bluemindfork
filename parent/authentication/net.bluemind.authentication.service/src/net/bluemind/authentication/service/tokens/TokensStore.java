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
import java.util.Collection;
import java.util.List;
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

import net.bluemind.authentication.service.Token;
import net.bluemind.common.hollow.BmFilesystemBlobStorageCleaner;

public class TokensStore {
	private static final String BASE_DATA_DIR = "/var/spool/bm-hollowed/tokens";
	private static final Logger logger = LoggerFactory.getLogger(TokensStore.class);

	private static final TokensStore INST = new TokensStore();

	public static TokensStore get() {
		return INST;
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
			producer.runCycle(state -> {
				state.add(new Token(net.bluemind.config.Token.admin0(), "admin0", "global.virt"));
			});
		}

		this.consumer = new HollowConsumer.Builder<>().withBlobRetriever(blobRetriever)
				.withGeneratedAPIClass(TokensAPI.class).build();
		consumer.triggerRefresh();

		this.keyIndex = UniqueKeyIndex.from(consumer, net.bluemind.authentication.service.tokens.Token.class)
				.usingPath("key", String.class);
		consumer.addRefreshListener(this.keyIndex);
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
		if (current != null) {
			long newVersion = incremental.runIncrementalCycle(incrementalState -> incrementalState.delete(current));
			consumer.triggerRefreshTo(newVersion);
		}
		return current;
	}

	public Token byKey(String key) {
		net.bluemind.authentication.service.tokens.Token internalTok = keyIndex.findMatch(key);
		if (internalTok != null) {
			Token tok = new Token(key, internalTok.getSubjectUid().getValue(),
					internalTok.getSubjectDomain().getValue());
			return tok;
		}
		return null;
	}

	public synchronized void expireOldTokens() {
		TokensAPI api = (TokensAPI) consumer.getAPI();
		Collection<net.bluemind.authentication.service.tokens.Token> tokens = api.getAllToken();
		long now = System.currentTimeMillis();
		List<net.bluemind.authentication.service.tokens.Token> expired = tokens.stream()
				.filter(tok -> now > tok.getExpiresTimestamp()).collect(Collectors.toList());
		if (!expired.isEmpty()) {
			long newVersion = incremental.runIncrementalCycle(incrementalState -> {
				for (net.bluemind.authentication.service.tokens.Token expiredToken : tokens) {
					incrementalState.delete(expiredToken);
				}
			});
			consumer.triggerRefreshTo(newVersion);
		}
		logger.info("Expired {} token(s), {} remaining.", expired.size(), tokens.size() - expired.size());
	}

}
