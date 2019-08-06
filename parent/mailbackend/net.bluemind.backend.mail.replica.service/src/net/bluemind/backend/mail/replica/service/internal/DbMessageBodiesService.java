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

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Header;
import net.bluemind.backend.mail.parsing.BodyStreamProcessor;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.MailApiHeaders;
import net.bluemind.backend.mail.replica.indexing.IndexedMessageBody;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.backend.mail.replica.persistence.MessageBodyStore;
import net.bluemind.backend.mail.replica.service.internal.BodyInternalIdCache.ExpectedId;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;

public class DbMessageBodiesService implements IDbMessageBodies {

	private static final Logger logger = LoggerFactory.getLogger(DbMessageBodiesService.class);

	protected final MessageBodyStore bodyStore;

	public DbMessageBodiesService(MessageBodyStore bs) {
		this.bodyStore = bs;
	}

	@Override
	public void create(String uid, Stream eml) {
		CompletableFuture<Void> promise = BodyStreamProcessor.processBody(eml).exceptionally(t -> {
			logger.error(t.getMessage(), t);
			return null;
		}).thenAccept(bodyData -> {
			MessageBody body = bodyData.body;
			if (body != null) {
				logger.debug("Got body '{}'", body.subject);
				body.guid = uid;
				Optional<Header> idHeader = body.headers.stream()
						.filter(h -> MailApiHeaders.X_BM_INTERNAL_ID.equals(h.name)).findAny();
				Optional<Header> prevHeader = body.headers.stream()
						.filter(h -> MailApiHeaders.X_BM_PREVIOUS_BODY.equals(h.name)).findAny();
				if (idHeader.isPresent()) {
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
				try {
					bodyStore.create(body);
					BodiesCache.bodies.put(body.guid, body);
					RecordIndexActivator.getIndexer().ifPresent(service -> {
						IndexedMessageBody indexData = IndexedMessageBody.createIndexBody(body.guid, bodyData);
						service.storeBody(indexData);
					});
				} catch (SQLException e) {
					throw ServerFault.sqlFault(e);
				}

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
		BodiesCache.bodies.invalidate(uid);
		try {
			bodyStore.delete(uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
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
			checkCopy.removeAll(exists);
			return ImmutableList.copyOf(checkCopy);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void update(MessageBody mb) {
		try {
			if (exists(mb.guid)) {
				bodyStore.update(mb);
			} else {
				bodyStore.create(mb);
			}
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
