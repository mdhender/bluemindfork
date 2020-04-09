/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.sds.proxy.mapping;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.backend.cyrus.partitions.CyrusBoxes;
import net.bluemind.backend.cyrus.partitions.CyrusBoxes.ReplicatedBox;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecordsPromise;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxesPromise;
import net.bluemind.backend.mail.replica.api.ImapBinding;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.config.Token;
import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.TopologyException;
import net.bluemind.sds.proxy.dto.JsMapper;
import net.bluemind.sds.proxy.dto.MappingRequest;
import net.bluemind.sds.proxy.dto.RawMapping;
import net.bluemind.sds.proxy.events.SdsAddresses;

public class MappingController extends AbstractVerticle {

	private static final Logger logger = LoggerFactory.getLogger(MappingController.class);

	public static class Build implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return false;
		}

		@Override
		public Verticle newInstance() {
			return new MappingController();
		}

	}

	private static class FolderMapping {
		private final Map<Long, String> uidToGuid = new ConcurrentHashMap<>();

		public Map<Long, String> uidToGuid() {
			return uidToGuid;
		}
	}

	private static final Cache<String, BoxAndFolder> cyrusToFolder = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES).build();

	private static final Map<String, FolderMapping> perFolderMapping = new ConcurrentHashMap<>();

	private VertxPromiseServiceProvider serviceProvider;

	@Override
	public void start() {
		this.serviceProvider = getProvider(vertx);

		vertx.eventBus().consumer(SdsAddresses.MAP, (Message<JsonObject> msgJs) -> {
			JsonObject js = msgJs.body();
			try {
				RawMapping toMap = JsMapper.get().readValue(js.encode(), RawMapping.class);
				map(toMap).whenComplete((v, ex) -> {
					if (ex != null) {
						logger.error(ex.getMessage(), ex);
						msgJs.fail(500, ex.getMessage());
					} else {
						msgJs.reply("Yeah");
					}
				});
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				msgJs.fail(500, e.getMessage());
			}
		});

		vertx.eventBus().consumer(SdsAddresses.QUERY, (Message<JsonObject> msgJs) -> {
			JsonObject js = msgJs.body();
			try {
				MappingRequest toMap = JsMapper.get().readValue(js.encode(), MappingRequest.class);
				mailboxToFolder(toMap.mailbox).thenCompose(baf -> {
					FolderMapping mapping = perFolderMapping.computeIfAbsent(baf.folder.uid, k -> new FolderMapping());
					JsonObject res = new JsonObject();
					IDbMailboxRecordsPromise foldersApi = serviceProvider.instance(IDbMailboxRecordsPromise.class,
							baf.folder.uid);

					if (toMap.uid == null) {
						// full mapping
						JsonArray fullFolder = new JsonArray();
						Set<Long> added = new HashSet<>();
						for (Entry<Long, String> entries : mapping.uidToGuid().entrySet()) {
							fullFolder.add(new JsonObject().put("u", entries.getKey()).put("g", entries.getValue()));
							added.add(entries.getKey());
						}
						// merge replication data
						return foldersApi.sortedIds(null).thenCompose(ids -> {
							return foldersApi.imapBindings(ids);
						}).thenApply(bindings -> {
							Set<Long> known = mapping.uidToGuid().keySet();
							for (ImapBinding ib : bindings) {
								if (!known.contains(ib.imapUid)) {
									fullFolder.add(new JsonObject().put("u", ib.imapUid).put("g", ib.bodyGuid));
								}
							}
							res.put("result", 200);
							res.put("mappings", fullFolder);
							return res;
						});
					} else {
						// single uid
						logger.info("Looking for {}", toMap.uid);
						String guid = mapping.uidToGuid().get(toMap.uid);
						if (guid != null) {
							res.put("result", 200);
							res.put("guid", guid);
							return CompletableFuture.completedFuture(res);
						} else {
							CompletableFuture<ItemValue<MailboxRecord>> lookup = foldersApi
									.getCompleteByImapUid(toMap.uid);
							return lookup.thenApply(found -> {
								if (found != null) {
									res.put("result", 200);
									res.put("guid", found.value.messageBody);
								} else {
									res.put("result", 404);
								}
								return res;
							});
						}
					}

				}).whenComplete((resJs, ex) -> {
					if (ex != null) {
						logger.error(ex.getMessage(), ex);
						msgJs.fail(500, ex.getMessage());
					} else {
						msgJs.reply(resJs);
					}
				});
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				msgJs.fail(500, e.getMessage());
			}
		});

		// mq listener on mailbox id replication, clear mapping
		vertx.eventBus().consumer("mapping.ctrl.discard", (Message<String> folderUniqueId) -> {
			String uid = folderUniqueId.body();
			Optional.ofNullable(perFolderMapping.get(uid)).ifPresent(mapping -> {
				logger.info("Forget {} mapping", uid);
				mapping.uidToGuid().clear();
			});
		});

	}

	private static class BoxAndFolder {
		public BoxAndFolder(ReplicatedBox box, ItemValue<MailboxFolder> folder) {
			this.box = box;
			this.folder = folder;
		}

		private ReplicatedBox box;
		private ItemValue<MailboxFolder> folder;
	}

	private CompletableFuture<BoxAndFolder> mailboxToFolder(String mailbox) {
		BoxAndFolder baf = cyrusToFolder.getIfPresent(mailbox);
		if (baf != null) {
			return CompletableFuture.completedFuture(baf);
		} else {
			ReplicatedBox asBox = CyrusBoxes.forCyrusMailbox(mailbox);
			IDbReplicatedMailboxesPromise foldersApi = serviceProvider.instance(IDbReplicatedMailboxesPromise.class,
					asBox.partition, asBox.ns.prefix() + asBox.local);
			return foldersApi.byName(asBox.folderName).thenApply(f -> {
				BoxAndFolder ret = new BoxAndFolder(asBox, f);
				cyrusToFolder.put(mailbox, ret);
				return ret;
			});
		}
	}

	private CompletableFuture<Void> map(RawMapping toMap) {
		logger.info("Mapping {} in {}", toMap.guid, toMap.cyrusMailbox);
		return mailboxToFolder(toMap.cyrusMailbox).thenAccept(boxAndFolder -> {
			logger.info("Resolved {} => {}", boxAndFolder.box.folderName, boxAndFolder.folder.uid);
			FolderMapping fm = perFolderMapping.computeIfAbsent(boxAndFolder.folder.uid, uid -> new FolderMapping());
			fm.uidToGuid().put(toMap.uid, toMap.guid);
			logger.info("{} mapped to {}", toMap.guid, toMap.uid);
		});
	}

	private static VertxPromiseServiceProvider getProvider(Vertx vertx) {
		ILocator cachingLocator = (String service, AsyncHandler<String[]> asyncHandler) -> {
			Optional<IServiceTopology> topology = Topology.getIfAvailable();
			if (topology.isPresent()) {
				String core = topology.get().core().value.address();
				String[] resp = new String[] { core };
				asyncHandler.success(resp);
			} else {
				asyncHandler.failure(new TopologyException("topology not available"));
			}
		};
		HttpClientProvider clientProvider = new HttpClientProvider(vertx);
		return new VertxPromiseServiceProvider(clientProvider, cachingLocator, Token.admin0());
	}

}
