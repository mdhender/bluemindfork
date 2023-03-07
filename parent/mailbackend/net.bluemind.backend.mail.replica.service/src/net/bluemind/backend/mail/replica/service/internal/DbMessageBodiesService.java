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
import java.util.Date;
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

import io.netty.buffer.ByteBufUtil;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.streams.ReadStream;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor.MessageBodyData;
import net.bluemind.backend.mail.replica.api.IMessageBodyTierChange;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.backend.mail.replica.service.IInternalDbMessageBodies;
import net.bluemind.backend.mail.replica.service.sds.MessageBodyObjectStore;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.rest.vertx.VertxStream.LocalPathStream;
import net.bluemind.index.mail.IndexableMessageBodyCache;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.sync.api.SdsSyncEvent;
import net.bluemind.sds.sync.api.SdsSyncEvent.Body;

public class DbMessageBodiesService implements IInternalDbMessageBodies {

	private static final Logger logger = LoggerFactory.getLogger(DbMessageBodiesService.class);

	protected final MessageBodyStore bodyStore;

	private final Supplier<MessageBodyObjectStore> bodyObjectStore;
	private final Supplier<IMessageBodyTierChange> bodyTierChangeService;
	private final EventBus eventBus;

	public DbMessageBodiesService(MessageBodyStore bodyStore, Supplier<MessageBodyObjectStore> bodyObjectStore,
			Supplier<IMessageBodyTierChange> bodyTierChangeService) {
		this.bodyStore = bodyStore;
		this.bodyObjectStore = bodyObjectStore;
		this.bodyTierChangeService = bodyTierChangeService;
		eventBus = VertxPlatform.eventBus();
	}

	private static final File TMP = new File(System.getProperty("java.io.tmpdir"));
	private static final OpenOptions TMP_OPTS = new OpenOptions().setCreate(true).setTruncateExisting(true)
			.setWrite(true);

	@Override
	public void create(String uid, Stream pristine) {
		_create(uid, new Date(), pristine);
	}

	@Override
	public void createWithDeliveryDate(String uid, Date deliveryDate, Stream pristine) {
		_create(uid, deliveryDate, pristine);
	}

	/*
	 * deliveryDate is used to choose the correct storage tier
	 */
	private void _create(String uid, Date deliveryDate, Stream pristine) {
		if (exists(uid)) {
			logger.warn("Skipping existing body {}", uid);
			VertxStream.sink(pristine).orTimeout(10, TimeUnit.SECONDS).join();
			return;
		}

		File tmpFile = new File(TMP, uid + "." + System.nanoTime());
		ReadStream<Buffer> classic = VertxStream.read(pristine);
		if (classic instanceof LocalPathStream lps) {
			tmpFile = lps.path().toFile();
			logger.info("Using local-stream from {} ({} byte(s))", tmpFile, tmpFile.length());
		} else {
			AsyncFile tmpStream = VertxPlatform.getVertx().fileSystem().openBlocking(tmpFile.getAbsolutePath(),
					TMP_OPTS);
			CompletableFuture<Void> prom = classic.pipeTo(tmpStream).toCompletionStage().toCompletableFuture()
					.orTimeout(10, TimeUnit.SECONDS);
			classic.resume();
			logger.info("Using netbased-stream {}", classic);
			prom.join();
		}

		logger.info("File copy of {} stream created ({} byte(s))", uid, tmpFile.length());

		Stream eml = VertxStream.localPath(tmpFile.toPath());
		MessageBodyObjectStore objectStore = bodyObjectStore.get();
		try {
			objectStore.store(uid, deliveryDate, tmpFile);
			parseAndIndex(uid, deliveryDate, eml);
			eventBus.publish(SdsSyncEvent.BODYADD.busName(),
					new Body(ByteBufUtil.decodeHexDump(uid), objectStore.dataLocation()).toJson(),
					new DeliveryOptions().setLocalOnly(true));
		} finally {
			tmpFile.delete(); // NOSONAR
		}

	}

	private void parseAndIndex(String uid, Date deliveryDate, Stream eml) {
		BodyStreamProcessor.processBody(eml).exceptionally(t -> {
			logger.error(t.getMessage(), t);
			return null;
		}).thenAccept(bodyData -> {
			MessageBody body = bodyData != null ? bodyData.body : null;
			if (body != null) {
				logger.debug("Got body '{}'", body.subject);
				body.guid = uid;
				body.created = deliveryDate == null ? new Date() : deliveryDate;
				updateAndIndex(bodyData);
			}
		}).orTimeout(10, TimeUnit.SECONDS).join();
	}

	@Override
	public void updateAndIndex(MessageBodyData bodyData) {
		IndexedMessageBody indexData = IndexedMessageBody.createIndexBody(bodyData.body.guid, bodyData);
		IndexableMessageBodyCache.bodies.put(indexData.uid, indexData);
		IndexableMessageBodyCache.sourceHolder.put(indexData.uid, indexData);
		RecordIndexActivator.getIndexer().ifPresent(service -> service.storeBody(indexData));
		update(bodyData.body);
	}

	@Override
	public void delete(String uid) {
		try {
			bodyStore.delete(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
		eventBus.publish(SdsSyncEvent.BODYDEL.busName(),
				new Body(ByteBufUtil.decodeHexDump(uid), bodyObjectStore.get().dataLocation()).toJson(),
				new DeliveryOptions().setLocalOnly(true));
		BodiesCache.bodies.invalidate(uid);
	}

	public MessageBody getComplete(String uid) {
		return BodiesCache.bodies.get(uid, t -> {
			try {
				return bodyStore.get(t);
			} catch (SQLException e) {
				throw new ServerFault(e);
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
			bodyTierChangeService.get().createBody(mb);
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

	@Override
	public MessageBody get(String uid) {
		return getComplete(uid);
	}

	@Override
	public void restore(ItemValue<MessageBody> item, boolean isCreate) {
		if (item.value != null) {
			update(item.value);
		}
	}

}
