/* BEGIN LICENSE
  * Copyright @Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.cyrus.replication.server.state;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Registry;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.cyrus.replication.server.Token;
import net.bluemind.backend.cyrus.replication.server.utils.LiteralTokens;
import net.bluemind.backend.cyrus.replication.server.utils.ReplicatedBoxes;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.replica.api.ICyrusReplicationArtifactsPromise;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecordsPromise;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxesPromise;
import net.bluemind.backend.mail.replica.api.MailboxAnnotation;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.backend.mail.replica.api.MailboxReplicaRootDescriptor;
import net.bluemind.backend.mail.replica.api.MailboxSub;
import net.bluemind.backend.mail.replica.api.QuotaRoot;
import net.bluemind.backend.mail.replica.api.SeenOverlay;
import net.bluemind.backend.mail.replica.api.SieveScript;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class ReplicationState {

	private static final Logger logger = LoggerFactory.getLogger(ReplicationState.class);

	private final StorageApiLink storage;

	private final Registry registry;
	private final Counter addMsgCounter;
	private final Counter addMsgCounterBytes;
	private final Counter recordUpdates;

	public ReplicationState(Vertx vertx, StorageApiLink storage) {
		this.storage = storage;
		this.registry = MetricsRegistry.get();
		IdFactory idf = new IdFactory("cyrus-replication", registry, ReplicationState.class);
		this.addMsgCounter = registry.counter(idf.name("applyMessage", "upstream", storage.remoteIp()));
		this.addMsgCounterBytes = registry.counter(idf.name("applyMessageBytes", "upstream", storage.remoteIp()));
		this.recordUpdates = registry.counter(idf.name("recordUpdates", "upstream", storage.remoteIp()));
		logger.debug("State created with vertx {}", vertx);
	}

	public CompletableFuture<Void> addMessage(MailboxMessage msg) {
		File dest = new File(Token.ROOT, msg.partition() + "_" + msg.guid() + ".eml");
		LiteralTokens.export(msg.content(), dest);
		long len = dest.length();
		return storage.bodies(msg.partition()).thenCompose(messageBodiesApi -> {
			try {
				Stream uploadStream = storage.stream(dest.toPath());
				return messageBodiesApi.create(msg.guid(), uploadStream);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}).whenComplete((v, ex) -> {
			if (!dest.delete()) {
				logger.debug("{} was not deleted", dest);
			}
			if (ex != null) {
				logger.error("addMessage.create: {}", ex.getMessage(), ex);
			} else {
				addMsgCounter.increment();
				addMsgCounterBytes.increment(len);
			}
		});
	}

	public CompletableFuture<MessageBody> messageByGuid(String partition, String guid) {
		return storage.bodies(partition).thenCompose(bodyApi -> {
			return bodyApi.getComplete(guid);
		});
	}

	public CompletableFuture<List<String>> missingGuids(String partition, List<String> guid) {
		List<String> missing = new ArrayList<>(guid.size());
		return storage.bodies(partition).thenCompose(bodyApi -> {
			CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
			for (List<String> part : Lists.partition(guid, 500)) {
				chain = chain.thenCompose(v -> {
					return bodyApi.missing(part).thenAccept(partMissing -> {
						logger.info("{} bodies missing out of {}", partMissing.size(), part.size());
						missing.addAll(partMissing);
					});
				});
			}
			return chain.thenApply(v -> missing);
		});
	}

	public CompletableFuture<MailboxFolder> folderByName(String name) {
		return foldersByName(Arrays.asList(name)).thenApply(resolved -> resolved.isEmpty() ? null : resolved.get(0));
	}

	public CompletableFuture<List<MailboxFolder>> foldersByName(List<String> names) {
		return storage.resolveNames(names).thenApply(resolved -> resolved.stream().map(v -> {
			try {
				MailboxFolder ret = DtoConverters.from(v.partition, v.desc, v.replica);
				ret.setAnnotations(v.annotations);
				return ret;
			} catch (Exception e) {
				logger.error("Resolved mailbox {} is incorrect ({}), skipping it.", v, e.getMessage());
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList()));
	}

	public CompletableFuture<Optional<Buffer>> record(MailboxFolder folder, String bodyGuid, long imapUid) {
		if (folder == null) {
			logger.warn("Null folder provided for loading {}/{}", bodyGuid, imapUid);
			return CompletableFuture.completedFuture(Optional.empty());
		}
		AtomicReference<IDbMailboxRecordsPromise> apiRef = new AtomicReference<>();
		return storage.mailboxRecords(folder.getUniqueId()).thenCompose(recApi -> {
			apiRef.set(recApi);
			return recApi.getCompleteByImapUid(imapUid);
		}).thenCompose((ItemValue<MailboxRecord> rec) -> {
			if (rec == null || !rec.value.messageBody.equals(bodyGuid)) {
				logger.warn("Not found or guid missmatch {} vs {}", rec, bodyGuid);
				throw new ServerFault("Not found or guid mismatch " + rec + " vs " + bodyGuid);
			} else {
				return apiRef.get().fetchComplete(rec.value.imapUid);
			}
		}).thenCompose(GenericStream::asyncStreamToBuffer).exceptionally(ex -> null).thenApply(Optional::ofNullable);
	}

	public CompletableFuture<List<MailboxFolder>> foldersByUser(String userName) {
		CompletableFuture<List<MailboxFolder>> ret = new CompletableFuture<>();
		ReplicatedBox box = ReplicatedBoxes.forLoginAtDomain(userName);
		if (box == null) {
			ret.completeExceptionally(ReplicationException.malformedMailboxName("user " + userName + " not found."));
			return ret;
		}
		return storage.replicatedMailboxes(box).thenCompose(apiDesc -> {
			return apiDesc.mboxApi.allReplicas().exceptionally(t -> {
				logger.error(t.getMessage(), t);
				return new LinkedList<>();
			}).thenApply(replicas -> {
				logger.info("Found {} folder(s)", replicas.size());
				return replicas.stream().map(rep -> DtoConverters.from(apiDesc.partition, apiDesc.rootDesc, rep))
						.collect(Collectors.toList());
			});
		});
	}

	public CompletableFuture<Void> quota(QuotaRoot sub) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		String[] splited = sub.root.split("!");
		String domain = splited[0];
		String boxName = splited[1];
		if (boxName.startsWith("user.")) {
			boxName = boxName.replaceFirst("user.", "");
		}
		String userId = boxName + "@" + domain;
		storage.cyrusArtifacts(userId).thenCompose(api -> {
			if (sub.limit == 0) {
				return api.deleteQuota(sub);
			} else {
				return api.storeQuota(sub);
			}
		}).whenComplete((any, ex) -> {
			if (ex != null) {
				logger.error(ex.getMessage(), ex);
			}
			ret.complete(null);
		});
		return ret;
	}

	public CompletableFuture<Void> annotate(MailboxAnnotation sub) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		storage.cyrusAnnotations().thenCompose(api -> {
			return api.storeAnnotation(sub);
		}).whenComplete((any, ex) -> {
			if (ex != null) {
				logger.error(ex.getMessage(), ex);
			}
			ret.complete(null);
		});
		return ret;
	}

	public CompletableFuture<List<QuotaRoot>> quotaByUser(String userName) {
		return storage.cyrusArtifacts(userName).thenCompose(api -> api.quotas());
	}

	public CompletableFuture<List<MailboxAnnotation>> annotationsByMailbox(String mbox) {
		return storage.cyrusAnnotations().thenCompose(api -> api.annotations(mbox));
	}

	public CompletableFuture<Void> sub(MailboxSub sub) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		storage.cyrusArtifacts(sub.userId).thenCompose(api -> {
			return api.storeSub(sub);
		}).whenComplete((any, ex) -> {
			if (ex != null) {
				logger.error(ex.getMessage(), ex);
			}
			ret.complete(null);
		});
		return ret;
	}

	public CompletableFuture<Void> unsub(MailboxSub sub) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		storage.cyrusArtifacts(sub.userId).thenCompose(api -> {
			return api.deleteSub(sub);
		}).whenComplete((any, ex) -> {
			if (ex != null) {
				logger.error(ex.getMessage(), ex);
			}
			ret.complete(null);
		});
		return ret;
	}

	public CompletableFuture<List<MailboxSub>> subByUser(String userName) {
		return storage.cyrusArtifacts(userName).thenCompose(api -> api.subs());
	}

	public CompletableFuture<Void> registerFolder(MailboxFolder folder) {
		if (logger.isDebugEnabled()) {
			logger.debug("**** register folder {}, part: {}", folder.getName(), folder.getPartition());
		}

		String partition = folder.getPartition();
		int mark = folder.getName().indexOf('!');
		String mboxName = folder.getName().substring(mark + 1);

		ReplicatedBox parsed = ReplicatedBoxes.forCyrusMailbox(folder.getName());
		MailboxReplicaRootDescriptor rootDesc = parsed.asDescriptor();
		return storage.replicatedMailboxes(partition, rootDesc).thenCompose(mboxesApi -> {
			logger.debug("Got API for storage: {}", mboxesApi.getClass());
			return mboxesApi.getComplete(folder.getUniqueId()).thenCompose(mboxReplicaIV -> {
				MailboxReplica replica = DtoConverters.from(rootDesc, mboxName, folder, parsed.ns);
				if (mboxReplicaIV == null) {
					return mboxesApi.create(folder.getUniqueId(), replica);
				} else {
					return mboxesApi.update(folder.getUniqueId(), replica);
				}
			}).thenCompose(v -> {
				if (folder.getAnnotations().isEmpty()) {
					return CompletableFuture.completedFuture(null);
				} else {
					logger.debug("Should save {} annotation(s)", folder.getAnnotations().size());
					return CompletableFuture.allOf(folder.getAnnotations().stream().map(anno -> annotate(anno))
							.toArray(CompletableFuture[]::new));
				}
			});
		});
	}

	public CompletableFuture<Void> sieve(SieveData sd) {
		SieveScript sieve = sd.script;
		sd.literalRef.ifPresent(litToken -> {
			File dest = new File(Token.ROOT, sieve.userId + "_" + sieve.fileName);
			LiteralTokens.export(litToken, dest);
			dest.delete();
		});
		return storage.cyrusArtifacts(sieve.userId).thenCompose(api -> {
			return api.storeScript(sieve);
		});
	}

	public CompletableFuture<Void> unsieve(SieveData sd) {
		SieveScript sieve = sd.script;
		sd.literalRef.ifPresent(litToken -> {
			File dest = new File(Token.ROOT, sieve.userId + "_" + sieve.fileName);
			LiteralTokens.export(litToken, dest);
			dest.delete();
		});
		return storage.cyrusArtifacts(sieve.userId).thenCompose(api -> api.deleteScript(sieve));
	}

	public CompletableFuture<List<SieveScript>> sieveByUser(String userName) {
		return storage.cyrusArtifacts(userName).thenCompose(api -> {
			return api.sieves();
		});
	}

	public CompletableFuture<Void> seenOverlay(SeenOverlay seen) {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		storage.cyrusArtifacts(seen.userId).thenCompose(api -> {
			return api.storeSeen(seen);
		}).whenComplete((any, ex) -> {
			if (ex != null) {
				logger.error(ex.getMessage(), ex);
			}
			ret.complete(null);
		});
		return ret;
	}

	public CompletableFuture<List<SeenOverlay>> seenOverlayByUser(String userName) {
		return storage.cyrusArtifacts(userName).thenCompose(ICyrusReplicationArtifactsPromise::seens);
	}

	public CompletableFuture<Void> updateRecords(String boxUniqueId, List<MailboxRecord> mboxState) {
		return storage.mailboxRecords(boxUniqueId).thenCompose(recApi -> {
			CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
			for (List<MailboxRecord> chunk : Lists.partition(mboxState, 200)) {
				chain = chain.thenCompose(v -> recApi.updates(chunk)).exceptionally(t -> {
					if (t instanceof ServerFault) {
						if (((ServerFault) t).getCode() == ErrorCode.TIMEOUT) {
							logger.info("Ignoring timeout {}", t.getMessage());
							return null;
						}
					} else if (t instanceof RuntimeException) {
						throw (RuntimeException) t;
					} else {
						throw new RuntimeException(t);
					}
					return null;
				}).thenAccept(v -> recordUpdates.increment(chunk.size()));
			}
			return chain;
		});

	}

	public CompletableFuture<Void> rename(String from, String to) {
		ReplicatedBox userFrom = ReplicatedBoxes.forCyrusMailbox(from);
		ReplicatedBox userTo = ReplicatedBoxes.forCyrusMailbox(to);
		if (userFrom == null || userTo == null) {
			CompletableFuture<Void> ret = new CompletableFuture<>();
			ret.completeExceptionally(ReplicationException
					.malformedMailboxName("mailbox(es) not found rename from: " + from + ", to: " + to));
			return ret;
		}
		return storage.replicatedMailboxes(userFrom).thenCompose(apiDesc -> {
			IDbReplicatedMailboxesPromise api = apiDesc.mboxApi;
			return api.byReplicaName(userFrom.fullName()).thenCompose(mbox -> {
				if (mbox == null) {
					logger.warn("Source not found for rename {}", userFrom);
					// consider throwing here...
					return CompletableFuture.completedFuture(null);
				}

				MailboxReplica value = mbox.value;
				value.fullName = userTo.fullName();
				value.parentUid = null;
				value.deleted = userTo.ns.expunged();

				return api.update(mbox.uid, value);
			});
		});
	}

	public CompletableFuture<Void> delete(String toDel) {
		ReplicatedBox userFrom = ReplicatedBoxes.forCyrusMailbox(toDel);
		if (userFrom == null) {
			CompletableFuture<Void> ret = new CompletableFuture<>();
			ret.completeExceptionally(ReplicationException.malformedMailboxName("mailbox " + toDel + " is malformed."));
			return ret;
		}
		return storage.replicatedMailboxes(userFrom).thenCompose(apiDesc -> {
			IDbReplicatedMailboxesPromise api = apiDesc.mboxApi;
			return api.byName(userFrom.fullName()).thenCompose(mbox -> {
				if (mbox == null) {
					logger.warn("Mailbox does not exist {}", userFrom);
					return CompletableFuture.completedFuture(null);
				}
				return api.delete(mbox.uid);
			}).thenApply(v -> {
				if (userFrom.mailboxRoot) {
					logger.warn("**** Deleting a MAILBOX ROOT {}, should drop the whole subtree", userFrom);
				}
				return null;
			});
		});
	}

	public CompletableFuture<List<MboxRecord>> records(MailboxFolder known) {
		return storage.mailboxRecords(known.getUniqueId()) //
				.thenCompose(recApi -> recApi.all() //
						.thenApply(records -> records.stream().map(r -> DtoConverters.from(r.value)).toList()))
				.exceptionally(e -> {
					logger.warn("Error while creating MBoxRecords", e);
					return Collections.emptyList();
				});
	}

	public CompletableFuture<Void> expunge(String mbox, List<Long> uid) {
		ReplicatedBox userFrom = ReplicatedBoxes.forCyrusMailbox(mbox);
		if (userFrom == null) {
			CompletableFuture<Void> ret = new CompletableFuture<>();
			ret.completeExceptionally(ReplicationException.malformedMailboxName("mailbox " + mbox + " not found."));
			return ret;
		}
		return storage.replicatedMailboxes(userFrom)
				.thenCompose(apiDesc -> apiDesc.mboxApi.byName(userFrom.folderName).thenAccept(mboxItem -> {
					if (mboxItem != null) {
						storage.mailboxRecords(mboxItem.uid)
								.thenAccept(recordsApi -> recordsApi.deleteImapUids(uid).whenComplete((v, ex) -> {
									if (ex != null) {
										logger.error(ex.getMessage(), ex);
									}
								}));
					}
				}));

	}

	public CompletableFuture<Boolean> checkCredentials(String login, String secret) {
		return storage.validate(login, secret);
	}

}
