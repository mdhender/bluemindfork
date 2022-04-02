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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.store.ITopicStore;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;
import net.bluemind.core.backup.continuous.store.RecordHandler;
import net.bluemind.core.backup.continuous.store.TopicManager;
import net.bluemind.core.backup.continuous.store.TopicPublisher;
import net.bluemind.core.backup.continuous.store.TopicSubscriber;

public class NoopStore {

	private static final Logger logger = LoggerFactory.getLogger(NoopStore.class);

	private NoopStore() {
		logger.debug("noop {}", this);
	}

	private static final class NoopToken implements IResumeToken {
		private static final NoopToken INST = new NoopToken();

		@Override
		public JsonObject toJson() {
			return new JsonObject();
		}
	}

	private static final TopicPublisher NOOP_PUBLISHER = new TopicPublisher() {

		@Override
		public CompletableFuture<Void> store(String partitionKey, byte[] key, byte[] data) {
			return CompletableFuture.completedFuture(null);
		}
	};

	private static final TopicSubscriber NOOP_SUBSCRIBER = new TopicSubscriber() {

		@Override
		public String topicName() {
			return "noop";
		}

		@Override
		public IResumeToken subscribe(IResumeToken index, RecordHandler de, IRecordStarvationStrategy start) {
			return index;
		}

		@Override
		public IResumeToken subscribe(IResumeToken index, RecordHandler de) {
			return index;
		}

		@Override
		public IResumeToken subscribe(RecordHandler de) {
			return NoopToken.INST;
		}

		@Override
		public IResumeToken parseToken(JsonObject js) {
			return NoopToken.INST;
		}
	};

	private static final TopicManager NOOP_MGT = new TopicManager() {

		@Override
		public void delete(String topic) {
			logger.warn("Noop manager will not delete {}", topic);
		}

		@Override
		public void reconfigure(String topic, Map<String, String> updatedProps) {
			logger.warn("Noop manager will not reconfigure {}", topic);
		}

		@Override
		public void flush(String topic) {
			logger.warn("Noop manager flush()");
		}
	};

	public static final ITopicStore NOOP = new ITopicStore() {

		@Override
		public Set<String> topicNames() {
			return Collections.emptySet();
		}

		@Override
		public Set<String> topicNames(String installationId) {
			return Collections.emptySet();
		}

		@Override
		public TopicPublisher getPublisher(TopicDescriptor td) {
			return NOOP_PUBLISHER;
		}

		@Override
		public TopicSubscriber getSubscriber(String topicName) {
			return NOOP_SUBSCRIBER;
		}

		@Override
		public TopicManager getManager() {
			return NOOP_MGT;
		}
	};

}
