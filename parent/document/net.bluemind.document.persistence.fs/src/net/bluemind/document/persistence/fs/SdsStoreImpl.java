/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.document.persistence.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.document.storage.IDocumentStore;
import net.bluemind.sds.dto.DeleteRequest;
import net.bluemind.sds.dto.ExistRequest;
import net.bluemind.sds.dto.ExistResponse;
import net.bluemind.sds.dto.GetRequest;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.dto.SdsResponse;
import net.bluemind.sds.store.ISdsSyncStore;

public class SdsStoreImpl implements IDocumentStore {
	private static final Logger logger = LoggerFactory.getLogger(SdsStoreImpl.class);

	private final ISdsSyncStore sds;

	public SdsStoreImpl(ISdsSyncStore syncSds) {
		this.sds = syncSds;
	}

	public static IDocumentStore create(ISdsSyncStore syncSds) {
		return new SdsStoreImpl(syncSds);
	}

	private String keyFromUid(String uid) {
		return "doc-fs-" + uid.replace('/', '_');
	}

	@Override
	public void store(String uid, byte[] content) throws ServerFault {
		logger.info("Pushing {} ...", keyFromUid(uid));

		Path tmpPath = null;
		try {
			tmpPath = Files.createTempFile("doc-fs", ".bin");
			Files.write(tmpPath, content, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			SdsResponse resp = sds.upload(PutRequest.of(keyFromUid(uid), tmpPath.toFile().getAbsolutePath()));
			if (!resp.succeeded()) {
				throw new ServerFault(resp.error.message);
			}
			logger.info("{} pushed to SDS", keyFromUid(uid));
		} catch (ServerFault sf) {
			throw sf;
		} catch (IOException e) {
			throw new ServerFault(e);
		} finally {
			if (tmpPath != null) {
				tmpPath.toFile().delete();
			}
		}
	}

	@Override
	public byte[] get(String uid) throws ServerFault {
		if (logger.isDebugEnabled()) {
			logger.debug("Try get '{}' aka {}", uid, keyFromUid(uid));
		}
		if (!exists(uid)) {
			return null;
		}
		Path tmpPath = null;
		try {
			tmpPath = Files.createTempFile("doc-fs", ".bin");
			SdsResponse resp = sds.download(GetRequest.of("docs", keyFromUid(uid), tmpPath.toFile().getAbsolutePath()));
			if (!resp.succeeded()) {
				throw new ServerFault(resp.error.message);
			}
			return Files.readAllBytes(tmpPath);
		} catch (ServerFault sf) {
			throw sf;
		} catch (IOException e) {
			throw new ServerFault(e);
		} finally {
			if (tmpPath != null) {
				tmpPath.toFile().delete();
			}
		}
	}

	@Override
	public void delete(String uid) throws ServerFault {
		sds.delete(DeleteRequest.of(keyFromUid(uid)));
	}

	@Override
	public boolean exists(String uid) throws ServerFault {
		ExistResponse resp = sds.exists(ExistRequest.of(keyFromUid(uid)));
		if (resp.succeeded()) {
			return resp.exists;
		} else {
			throw new ServerFault(resp.error.message);
		}
	}

	@Override
	public int getPriority() {
		return 1;
	}

}
