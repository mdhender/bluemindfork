/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.backend.mail.replica.service.internal.BodyInternalIdCache.ExpectedId;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.rest.vertx.VertxStream.LocalPathStream;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.state.StateContext;

public class DbMessageBodiesService implements IDbMessageBodies {

	private static final Logger logger = LoggerFactory.getLogger(DbMessageBodiesService.class);

	protected final MessageBodyStore bodyStore;
	private final Supplier<MessageBodyObjectStore> bodyObjectStore;

	public DbMessageBodiesService(MessageBodyStore bodyStore, Supplier<MessageBodyObjectStore> bodyObjectStore) {
		this.bodyStore = bodyStore;
		this.bodyObjectStore = bodyObjectStore;
	}

	private static final File TMP = new File(System.getProperty("java.io.tmpdir"));
	private static final OpenOptions TMP_OPTS = new OpenOptions().setCreate(true).setTruncateExisting(true)
			.setWrite(true);

	@Override
	public void create(String uid, Stream pristine) {
		if (exists(uid)) {
			try {
				logger.warn("Skipping existing body {}", uid);
				VertxStream.sink(pristine).get(10, TimeUnit.SECONDS);
				return;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw new ServerFault(e);
			}
		}

		File tmpFile = new File(TMP, uid + "." + System.nanoTime());
		ReadStream<Buffer> classic = VertxStream.read(pristine);
		if (classic instanceof LocalPathStream) {
			LocalPathStream lps = (LocalPathStream) classic;
			tmpFile = lps.path().toFile();
		} else {
			AsyncFile tmpStream = VertxPlatform.getVertx().fileSystem().openBlocking(tmpFile.getAbsolutePath(),
					TMP_OPTS);
			CompletableFuture<Void> prom = classic.pipeTo(tmpStream).toCompletionStage().toCompletableFuture();
			classic.resume();
			prom.join();
		}

		logger.info("File copy of {} stream created.", uid);

		Stream eml = VertxStream.localPath(tmpFile.toPath());
		MessageBodyObjectStore objectStore = bodyObjectStore.get();
		try {
			objectStore.store(uid, tmpFile);
			parseAndIndex(uid, eml);
		} finally {
			tmpFile.delete(); // NOSONAR
		}

	}

	private void parseAndIndex(String uid, Stream eml) {
		CompletableFuture<Void> promise = BodyStreamProcessor.processBody(eml).exceptionally(t -> {
			logger.error(t.getMessage(), t);
			return null;
		}).thenAccept(bodyData -> {
			MessageBody body = bodyData != null ? bodyData.body : null;
			if (body != null) {
				logger.debug("Got body '{}'", body.subject);
				body.guid = uid;
				Optional<Header> idHeader = body.headers.stream()
						.filter(h -> MailApiHeaders.X_BM_INTERNAL_ID.equals(h.name)).findAny();
				Optional<Header> prevHeader = body.headers.stream()
						.filter(h -> MailApiHeaders.X_BM_PREVIOUS_BODY.equals(h.name)).findAny();
				if (idHeader.isPresent() && StateContext.getState() != SystemState.CORE_STATE_CLONING) {
					String idStr = idHeader.get().firstValue();
					int instIdx = idStr.lastIndexOf(':');
					int ownerIdx = idStr.lastIndexOf('#');
					if (instIdx > 0 && ownerIdx > 0 && ownerIdx < instIdx) {
						String owner = idStr.substring(0, ownerIdx);
						String instId = idStr.substring(ownerIdx + 1, instIdx);
						if (InstallationId.getIdentifier().equals(instId)) {
							long internalId = Long.parseLong(idStr.substring(instIdx + 1));
							String prevBody = null;
							if (prevHeader.isPresent()) {
								prevBody = prevHeader.get().firstValue();
							}
							logger.warn("********** caching {} => {} for owner {}", body.guid, internalId, owner);
							BodyInternalIdCache.storeExpectedRecordId(body.guid,
									new ExpectedId(internalId, owner, prevBody));
						}
					}
				}
				update(body);
				RecordIndexActivator.getIndexer().ifPresent(service -> {
					IndexedMessageBody indexData = IndexedMessageBody.createIndexBody(body.guid, bodyData);
					service.storeBody(indexData);
				});
			}
		});
		try {
			promise.get(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new ServerFault(e);
		}
	}

	@Override
	public void delete(String uid) {
		try {
			bodyStore.delete(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		BodiesCache.bodies.invalidate(uid);
	}

	public MessageBody getComplete(String uid) {
		return Optional.ofNullable(BodiesCache.bodies.getIfPresent(uid)).orElseGet(() -> {
			try {
				return bodyStore.get(uid);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
		});
	}

	@Override
	public boolean exists(String uid) {
		MessageBody existing = BodiesCache.bodies.getIfPresent(uid);
		if (existing != null) {
			return true;
		}
		try {
			return bodyStore.exists(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<String> missing(List<String> toCheck) {
		try {
			List<String> notNull = Optional.ofNullable(toCheck).orElse(Collections.emptyList());
			List<String> exists = bodyStore.existing(notNull);
			Set<String> checkCopy = new HashSet<>(notNull);
			exists.forEach(checkCopy::remove);

			// check if the unknown bodies are not in our object store
			// and process them from here if they are
			long time = System.currentTimeMillis();
			MessageBodyObjectStore sdsStore = bodyObjectStore.get();
			Set<String> inObjectStore = sdsStore.exist(checkCopy);
			Set<String> processedFromObjectStore = new HashSet<>();
			for (List<String> slice : Lists.partition(new ArrayList<>(inObjectStore), 25)) {
				String[] guids = slice.toArray(new String[slice.size()]);
				Path[] fromSds = sdsStore.mopen(guids);
				for (int i = 0; i < guids.length; i++) {
					String guid = guids[i];
					Path tmpFromSDS = fromSds[i];
					if (processSdsItem(guid, tmpFromSDS)) {
						processedFromObjectStore.add(guid);
					}
				}
			}

			time = System.currentTimeMillis() - time;
			if (!processedFromObjectStore.isEmpty()) {
				checkCopy.removeAll(processedFromObjectStore);
				logger.info("{} message(s) processed from object-store in {}ms.", processedFromObjectStore.size(),
						time);
			}

			return ImmutableList.copyOf(checkCopy);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	private boolean processSdsItem(String guid, Path tmpFromSDS) {
		try {
			if (tmpFromSDS != null) {
				Stream emlFromObjectStore = VertxStream.localPath(tmpFromSDS);
				logger.debug("Process {} from object-store...", guid);
				create(guid, emlFromObjectStore);
				logger.debug("{} processed from object store !", guid);
				return true;
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (tmpFromSDS != null) {
				try {
					Files.deleteIfExists(tmpFromSDS);
				} catch (IOException e) {
					// ok
				}
			}
		}
		return false;
	}

	@Override
	public void update(MessageBody mb) {
		try {
			bodyStore.store(mb);
			BodiesCache.bodies.put(mb.guid, mb);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<MessageBody> multiple(List<String> uid) {
		try {
			return bodyStore.multiple(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

}
