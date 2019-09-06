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
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.sds.proxy.events;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.sds.proxy.dto.DeleteRequest;
import net.bluemind.sds.proxy.dto.ExistRequest;
import net.bluemind.sds.proxy.dto.GetRequest;
import net.bluemind.sds.proxy.dto.JsMapper;
import net.bluemind.sds.proxy.dto.PutRequest;
import net.bluemind.sds.proxy.dto.SdsRequest;
import net.bluemind.sds.proxy.dto.SdsResponse;
import net.bluemind.sds.proxy.store.ISdsBackingStore;
import net.bluemind.sds.proxy.store.ISdsBackingStoreFactory;
import net.bluemind.sds.proxy.store.dummy.DummyBackingStore;

public class SdsEventHandlerVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(SdsEventHandlerVerticle.class);

	public static class SdsEventFactory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SdsEventHandlerVerticle();
		}

	}

	private ISdsBackingStore sdsStore;
	private final Map<String, ISdsBackingStoreFactory> factories;
	private JsonObject storeConfig;

	public SdsEventHandlerVerticle() {
		this.factories = loadStoreFactories();
		this.storeConfig = new JsonObject();
		sdsStore = loadStore();
	}

	private ISdsBackingStore loadStore() {
		String storeType = storeConfig.getString("storeType");
		if (storeType == null || storeType.equals("dummy") || !factories.containsKey(storeType)) {
			logger.info("Defaulting to dummy store (requested: {})", storeType);
			return DummyBackingStore.FACTORY.create(vertx, storeConfig);
		} else {
			logger.info("Loading store {}", storeType);
			return factories.get(storeType).create(vertx, storeConfig);
		}

	}

	private Map<String, ISdsBackingStoreFactory> loadStoreFactories() {
		RunnableExtensionLoader<ISdsBackingStoreFactory> rel = new RunnableExtensionLoader<>();
		List<ISdsBackingStoreFactory> stores = rel.loadExtensions("net.bluemind.sds.proxy", "store", "store",
				"factory");
		logger.info("Found {} backing store(s)", stores.size());
		return stores.stream().collect(Collectors.toMap(f -> f.name(), f -> f));
	}

	@Override
	public void start() {

		UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
		UserPrincipal cyrusUser = null;
		GroupPrincipal mailGroup = null;
		try {
			cyrusUser = lookupService.lookupPrincipalByName("cyrus");
			mailGroup = lookupService.lookupPrincipalByGroupName("mail");
			logger.info("Found cyrus user {}, group {}", cyrusUser, mailGroup);
		} catch (IOException e) {
			logger.warn("Error looking up cyrus user: {}", e.getMessage());
		}
		final Optional<UserPrincipal> optCyrusUser = Optional.ofNullable(cyrusUser);
		final Optional<GroupPrincipal> optMailGroup = Optional.ofNullable(mailGroup);

		registerForJsonSdsRequest(SdsAddresses.EXIST, ExistRequest.class, sdsStore::exists);

		registerForJsonSdsRequest(SdsAddresses.DELETE, DeleteRequest.class, sdsStore::delete);

		registerForJsonSdsRequest(SdsAddresses.PUT, PutRequest.class, sdsStore::upload);

		registerForJsonSdsRequest(SdsAddresses.GET, GetRequest.class, get -> {
			SdsResponse resp = sdsStore.download(get);
			if (resp.succeeded()) {
				optCyrusUser.ifPresent(cyrus -> {
					optMailGroup.ifPresent(mail -> {
						mkdirAndChown(new File(get.filename), cyrus, mail);
					});
				});
			}
			return resp;
		});

	}

	private void mkdirAndChown(File dest, UserPrincipal user, GroupPrincipal g) {
		try {
			createParentDirs(dest, user, g);
			PosixFileAttributeView view = Files.getFileAttributeView(dest.toPath(), PosixFileAttributeView.class,
					LinkOption.NOFOLLOW_LINKS);
			view.setOwner(user);
			view.setGroup(g);
			if (logger.isDebugEnabled()) {
				logger.debug("{} owner set to {} {}", dest.getAbsolutePath(), user, g);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

	private void createParentDirs(File dest, UserPrincipal user, GroupPrincipal g) throws IOException {
		File parentDir = dest.getParentFile();
		if (!parentDir.exists()) {
			Files.createDirectories(parentDir.toPath(),
					PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---")));
			while (!parentDir.getAbsolutePath().equals("/var/spool/cyrus/data")) {
				PosixFileAttributeView view = Files.getFileAttributeView(parentDir.toPath(),
						PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
				view.setOwner(user);
				view.setGroup(g);
				parentDir = parentDir.getParentFile();
			}
		}
	}

	private static interface UnsafeFunction<T, R> {
		R apply(T param) throws Exception;
	}

	private <T extends SdsRequest, R extends SdsResponse> void registerForJsonSdsRequest(String address,
			Class<T> reqType, UnsafeFunction<T, R> process) {
		vertx.eventBus().registerHandler(address, (Message<JsonObject> msg) -> {
			String jsonString = msg.body().encode();
			try {
				T sdsReq = JsMapper.get().readValue(jsonString, reqType);
				R sdsResp = process.apply(sdsReq);
				JsonObject jsResp = new JsonObject(JsMapper.get().writeValueAsString(sdsResp));
				msg.reply(jsResp);
			} catch (Exception e) {
				logger.error("{} Error processing payload {}", address, jsonString, e);
				// let the event bus timeout trigger, an http 500 will be returned
			}
		});
	}

}
