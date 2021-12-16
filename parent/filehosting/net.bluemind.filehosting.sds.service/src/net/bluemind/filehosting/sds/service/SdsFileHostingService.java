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
package net.bluemind.filehosting.sds.service;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.filehosting.api.FileHostingInfo;
import net.bluemind.filehosting.api.FileHostingInfo.Type;
import net.bluemind.filehosting.api.FileHostingItem;
import net.bluemind.filehosting.api.FileHostingPublicLink;
import net.bluemind.filehosting.api.FileType;
import net.bluemind.filehosting.service.export.IFileHostingService;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class SdsFileHostingService implements IFileHostingService {

	private static final Logger logger = LoggerFactory.getLogger(SdsFileHostingService.class);
	private static final Supplier<ISdsSyncStore> sds = Suppliers.memoizeWithExpiration(
			() -> new DefaultSdsStoreLoader().forSysconf(LocalSysconfCache.get()).orElse(null), 5, TimeUnit.MINUTES);

	public SdsFileHostingService() {
		if (logger.isDebugEnabled()) {
			logger.debug("SDS provided by {}", sds);
		}
	}

	@Override
	public List<FileHostingItem> list(SecurityContext context, String path) throws ServerFault {
		return Collections.emptyList();
	}

	@Override
	public List<FileHostingItem> find(SecurityContext context, String query) throws ServerFault {
		return Collections.emptyList();
	}

	private String pathToUid(String path) {
		JsonObject js = new JsonObject();
		js.put("path", path);
		return "sds-" + Base64.getUrlEncoder().encodeToString(js.encode().getBytes());
	}

	private String uidToPath(String uid) {
		String b64 = uid.substring("sds-".length());
		JsonObject js = new JsonObject(new String(Base64.getUrlDecoder().decode(b64)));
		return js.getString("path");
	}

	private void ensureSdsIsAvailable() {
		if (sds.get() == null) {
			throw new ServerFault("SDS is not available");
		}
	}

	@Override
	public Stream get(SecurityContext context, String path) throws ServerFault {
		ensureSdsIsAvailable();
		String uid = pathToUid(path);
		logger.info("get(path: {} (aka uid {}))", path, uid);
		return download(uid);
	}

	private Stream download(String uid) {
		try {
			File tmp = File.createTempFile("sds", ".tmp");
			GetRequest get = GetRequest.of(null, uid, tmp.getAbsolutePath());
			SdsResponse dl = sds.get().download(get);
			logger.info("Download uid {} (path {}) => {}", uid, uidToPath(uid), dl);
			Vertx vx = VertxPlatform.getVertx();
			AsyncFile vxStream = vx.fileSystem().openBlocking(tmp.getAbsolutePath(), new OpenOptions().setRead(true));

			return VertxStream.stream(vxStream, end -> {
				logger.debug("Stream has ending, closing then clearing tmp file {}", tmp);
				vx.setTimer(50, tid -> {
					logger.debug("Closing in timer {}", tid);
					vxStream.close(ar -> {
						if (ar.failed()) {
							logger.warn("failed to close asyncFile", ar.cause());
						}
						tmp.delete(); // NOSONAR
					});
				});
			});
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public boolean exists(SecurityContext context, String path) throws ServerFault {
		ensureSdsIsAvailable();
		String uid = pathToUid(path);
		ExistRequest er = ExistRequest.of(uid);
		ExistResponse resp = sds.get().exists(er);
		return resp.exists;
	}

	@Override
	public FileHostingPublicLink share(SecurityContext context, String path, Integer downloadLimit,
			String expirationDate) throws ServerFault {
		String uid = pathToUid(path);
		FileHostingPublicLink link = new FileHostingPublicLink();
		link.url = String.format("%s/fh/bm-fh/%s", getServerAddress(), uid);
		return link;
	}

	private String getServerAddress() {
		SystemConf sysconf = LocalSysconfCache.get();
		return String.format("%s://%s", sysconf.values.getOrDefault(SysConfKeys.external_protocol.name(), "https"),
				sysconf.values.getOrDefault(SysConfKeys.external_url.name(), "configure.your.external.url"));
	}

	@Override
	public void unShare(SecurityContext context, String url) throws ServerFault {
		logger.warn("Unshare of {} is not possible with this implementation.", url);
	}

	@Override
	public void store(SecurityContext context, String path, Stream document) throws ServerFault {
		ensureSdsIsAvailable();
		String uid = pathToUid(path);
		ReadStream<Buffer> stream = VertxStream.read(document);
		try {
			File tmp = File.createTempFile("sds", ".tmp");
			CompletableFuture<Void> comp = new CompletableFuture<>();
			stream.pipeTo(VertxPlatform.getVertx().fileSystem().openBlocking(tmp.getAbsolutePath(),
					new OpenOptions().setWrite(true).setTruncateExisting(true).setCreate(true)), finished -> {
						if (finished.succeeded()) {
							comp.complete(null);
						} else {
							comp.completeExceptionally(finished.cause());
						}
					});
			comp.whenComplete((v, ex) -> {
				if (ex == null) {
					logger.info("Got {}, pushing {}byte(s) to sds", uid, tmp.length());
					PutRequest pr = PutRequest.of(uid, tmp.getAbsolutePath());
					sds.get().upload(pr);
				}
				tmp.delete();
			}).exceptionally(ex -> null).join();
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	@Override
	public void delete(SecurityContext context, String path) throws ServerFault {
		logger.warn("delete is bad; keeping {} around.", path);
	}

	@Override
	public FileHostingItem getComplete(SecurityContext context, String uid) throws ServerFault {
		FileHostingItem fh = new FileHostingItem();
		fh.path = uidToPath(uid);
		fh.name = new File(fh.path).getName();
		fh.type = FileType.FILE;
		fh.metadata = Collections.emptyList();
		return fh;
	}

	@Override
	public Stream getSharedFile(SecurityContext context, String uid) throws ServerFault {
		return download(uid);
	}

	@Override
	public FileHostingInfo info(SecurityContext context) throws ServerFault {
		FileHostingInfo info = new FileHostingInfo();
		info.info = "BlueMind SDS filehosting";
		info.type = Type.INTERNAL;
		info.present = sds.get() != null;
		logger.info("FileHostingInfo {} present {}", info.info, info.present);
		return info;
	}

	@Override
	public boolean supports(SecurityContext context) {
		return sds.get() != null;
	}

}
