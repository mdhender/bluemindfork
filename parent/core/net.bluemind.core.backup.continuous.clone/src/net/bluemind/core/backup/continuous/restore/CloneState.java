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
package net.bluemind.core.backup.continuous.restore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.store.ITopicStore.IResumeToken;

public class CloneState {

	private static final Logger logger = LoggerFactory.getLogger(CloneState.class);
	private Map<String, IResumeToken> topicNameResume;
	private Map<String, IResumeToken> freshTopicNameResume;
	private Path path;

	public CloneState(Path p, ILiveStream any) throws IOException {
		this.topicNameResume = new HashMap<>();
		this.freshTopicNameResume = new ConcurrentHashMap<>();
		this.path = p;
		if (p.toFile().exists()) {
			try {
				byte[] content = Files.readAllBytes(p);
				JsonObject js = new JsonObject(Buffer.buffer(content));
				for (String top : js.fieldNames()) {
					JsonObject jsTok = js.getJsonObject(top);
					IResumeToken tok = any.parse(jsTok);

					topicNameResume.put(top, tok);
				}
			} catch (IOException e) {
				logger.warn("clone state retrieval ({} {})", e.getClass(), e.getMessage());
			}
		} else {
			logger.warn("No previous clone state at {}", p);
		}

	}

	public CloneState clear() {
		topicNameResume.clear();
		freshTopicNameResume.clear();
		return this;
	}

	private static class EmptyState implements IResumeToken {
		private static final IResumeToken INST = new EmptyState();

		@Override
		public JsonObject toJson() {
			return new JsonObject();
		}

	}

	public IResumeToken forTopic(ILiveStream installationsStream) {
		String name = installationsStream.fullName();
		IResumeToken rt = freshTopicNameResume.get(name);
		if (rt == null || rt == EmptyState.INST) {
			rt = topicNameResume.get(installationsStream.fullName());
		}
		if (rt == null) {
			logger.warn("No recorded state for {}", name);
		}
		return rt;
	}

	public CloneState track(String fullName, IResumeToken index) {
		if (index == null) {
			logger.warn("Saving empty state for {}", fullName);
		}
		freshTopicNameResume.put(fullName, Optional.ofNullable(index).orElse(EmptyState.INST));
		return this;
	}

	public void save() {
		Map<String, IResumeToken> merged = new HashMap<>();
		merged.putAll(topicNameResume);
		merged.putAll(freshTopicNameResume);
		JsonObject newState = new JsonObject();
		for (Entry<String, IResumeToken> entry : merged.entrySet()) {
			IResumeToken value = entry.getValue();
			if (value == EmptyState.INST) {
				continue;
			}
			newState.put(entry.getKey(), value.toJson());
		}
		String encState = newState.encodePrettily();
		byte[] toStore = encState.getBytes();
		try {
			Files.write(path, toStore, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			logger.warn("clone state persistence ({})", e.getMessage());
		}
	}

}
