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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hollow.api.consumer.HollowConsumer;
import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;
import com.netflix.hollow.api.consumer.HollowConsumer.BlobRetriever;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemAnnouncementWatcher;
import com.netflix.hollow.api.consumer.fs.HollowFilesystemBlobRetriever;
import com.netflix.hollow.api.producer.HollowIncrementalProducer;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.BlobStorageCleaner;
import com.netflix.hollow.api.producer.fs.HollowFilesystemPublisher;
import com.netflix.hollow.core.write.objectmapper.RecordPrimaryKey;

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
	private final TokenPrimaryKeyIndex keyIndex;
	private final HollowIncrementalProducer incremental;

	private TokensStore() {
		File localPublishDir = new File(BASE_DATA_DIR);
		localPublishDir.mkdirs();

		HollowFilesystemPublisher publisher = new HollowFilesystemPublisher(localPublishDir.toPath());

		BlobStorageCleaner cleaner = new BmFilesystemBlobStorageCleaner(localPublishDir, 10);
		HollowProducer producer = HollowProducer.withPublisher(publisher) //
				.withBlobStorageCleaner(cleaner).build();
		producer.initializeDataModel(Token.class);
		this.incremental = new HollowIncrementalProducer(producer);

		HollowConsumer.BlobRetriever blobRetriever = new HollowFilesystemBlobRetriever(localPublishDir.toPath());
		if (!restoreIfAvailable(producer, blobRetriever,
				new HollowFilesystemAnnouncementWatcher(localPublishDir.toPath()))) {
			producer.runCycle(state -> {
				state.add(new Token(net.bluemind.config.Token.admin0(), "admin0", "global.virt"));
			});
		}

		this.consumer = HollowConsumer.withBlobRetriever(blobRetriever).withGeneratedAPIClass(TokensAPI.class).build();
		consumer.triggerRefresh();
		this.keyIndex = new TokenPrimaryKeyIndex(consumer, true, "key");
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
		incremental.addOrModify(tok);
		consumer.triggerRefreshTo(incremental.runCycle());
	}

	public synchronized Token remove(String key) {
		Token current = byKey(key);
		if (current != null) {
			incremental.delete(new RecordPrimaryKey("Token", new String[] { key }));
			consumer.triggerRefreshTo(incremental.runCycle());
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
		int count = 0;
		for (net.bluemind.authentication.service.tokens.Token tok : tokens) {
			if (now > tok.getExpiresTimestamp()) {
				incremental.delete(new RecordPrimaryKey("Token", new String[] { tok.getKey().getValue() }));
				count++;
			}
		}
		if (count > 0) {
			consumer.triggerRefreshTo(incremental.runCycle());
		}
		logger.info("Expired {} token(s), {} remaining.", count, tokens.size() - count);
	}

}
