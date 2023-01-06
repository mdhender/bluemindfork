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
package net.bluemind.backend.mail.replica.service.sds;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.bluemind.backend.mail.replica.api.TierMove;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.network.topology.Topology;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.MgetRequest;
import net.bluemind.sds.dto.MgetRequest.Transfer;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.dto.TierMoveRequest;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.sds.store.loader.SdsStoreLoader;
import net.bluemind.sds.store.noop.NoopStoreFactory;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class MessageBodyObjectStore {

	private static final Logger logger = LoggerFactory.getLogger(MessageBodyObjectStore.class);
	private final BmContext ctx;
	private final ISdsSyncStore objectStore;
	private boolean singleNamespaceBody;
	private final String dataLocation;

	public MessageBodyObjectStore(BmContext ctx, String datalocation) {
		this.ctx = ctx;
		this.dataLocation = datalocation;
		SystemConf config = LocalSysconfCache.get();
		SdsStoreLoader loader = new SdsStoreLoader();
		this.singleNamespaceBody = Topology.get().singleNode() || !loader.archiveKind(config).isShardedByDatalocation();
		this.objectStore = loader.forSysconf(config, datalocation).orElseGet(() -> {
			logger.warn("[{}] Returning noop store which might be wrong", datalocation);
			return new NoopStoreFactory().createSync(VertxPlatform.getVertx(), config, datalocation);
		});
	}

	public MessageBodyObjectStore(BmContext ctx, ISdsSyncStore objectStore, String datalocation) {
		this.ctx = ctx;
		this.objectStore = objectStore;
		this.dataLocation = datalocation;
		if (logger.isDebugEnabled()) {
			logger.debug("Object store for {}: {}", this.ctx, objectStore);
		}
	}

	/**
	 * Only one namespace for all body guid's (eg. single S3 bucket for all)
	 * 
	 * @return true if object store ignores datalocation
	 */
	public boolean isSingleNamespaceBody() {
		return singleNamespaceBody;
	}

	/**
	 * Returns a sub-collections containing only the GUIDs that our known in the
	 * object store
	 * 
	 * @param bodyGuid
	 * @return
	 */
	public Set<String> exist(Set<String> guids) {
		logger.debug("Checking {} with {}", guids, objectStore);
		return guids.stream().filter(guid -> objectStore.exists(ExistRequest.of(guid)).exists)
				.collect(Collectors.toSet());

	}

	public Path open(String guid) {
		logger.debug("Open {} with {}", guid, objectStore);
		Path target = null;
		try {
			target = Files.createTempFile(guid, ".s3");
		} catch (IOException e1) {
			throw new ServerFault(e1);
		}
		try {
			SdsResponse resp = objectStore.download(GetRequest.of("", guid, target.toString()));
			if (resp.succeeded()) {
				return target;
			} else {
				throw new ServerFault(resp.error.message);
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	public ByteBuf openMmap(String guid) {
		Path tgt = open(guid);
		try (FileChannel channel = FileChannel.open(tgt, StandardOpenOption.READ)) {
			MappedByteBuffer map = channel.map(MapMode.READ_ONLY, 0, Files.size(tgt));
			ByteBuf wrapped = Unpooled.wrappedBuffer(map);
			Files.delete(tgt);
			return wrapped.readerIndex(0);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
	}

	public Path[] mopen(String[] guids) {
		if (guids.length == 1) {
			return new Path[] { open(guids[0]) };
		}
		logger.debug("Open {} with {}", guids, objectStore);
		MgetRequest mgetReq = new MgetRequest();
		mgetReq.transfers = new ArrayList<>();
		ArrayList<Path> paths = new ArrayList<>();
		for (String guid : guids) {
			try {
				Path tempPath = Files.createTempFile(guid, ".s3");
				tempPath = tempPath.toAbsolutePath();
				paths.add(tempPath);
				mgetReq.transfers.add(Transfer.of(guid, tempPath.toString()));
			} catch (IOException e) {
				throw new ServerFault(e);
			}
		}

		try {
			objectStore.downloads(mgetReq);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
		return paths.toArray(new Path[] {});
	}

	public void delete(List<String> guids) {
		guids.stream().forEach(guid -> {
			objectStore.delete(DeleteRequest.of(guid));
		});
	}

	public void store(String uid, Date deliveryDate, File tmpFile) {
		logger.info("Store {} with {}", uid, objectStore);
		PutRequest pr = new PutRequest();
		pr.filename = tmpFile.getAbsolutePath();
		pr.guid = uid;
		pr.deliveryDate = deliveryDate;
		objectStore.upload(pr);
	}

	public List<String> tierMove(List<TierMove> tierMoves) {
		return objectStore.tierMove(new TierMoveRequest(tierMoves)).moved;
	}

	public String dataLocation() {
		return dataLocation;
	}
}
