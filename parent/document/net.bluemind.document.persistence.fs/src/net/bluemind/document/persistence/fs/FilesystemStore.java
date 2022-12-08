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

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.document.storage.IDocumentStore;
import net.bluemind.sds.store.ISdsSyncStore;
import net.bluemind.sds.store.loader.SdsDocumentStoreLoader;
import net.bluemind.system.sysconf.helper.LocalSysconfCache;

public class FilesystemStore implements IDocumentStore {
	private static final Logger logger = LoggerFactory.getLogger(FilesystemStore.class);

	private final Supplier<IDocumentStore> delegate;

	public FilesystemStore() {
		this.delegate = Suppliers.memoizeWithExpiration(this::selectStoreStrategy, 1, TimeUnit.MINUTES);
		logger.info("Selected delegate is {}", delegate);
	}

	private IDocumentStore selectStoreStrategy() {
		Optional<ISdsSyncStore> optSds = new SdsDocumentStoreLoader().forSysconf(LocalSysconfCache.get(), "unused");
		return optSds.map(SdsStoreImpl::create).orElseGet(FilesystemStoreImpl::new);
	}

	@Override
	public void store(String uid, byte[] content) throws ServerFault {
		delegate.get().store(uid, content);
	}

	@Override
	public byte[] get(String uid) throws ServerFault {
		return delegate.get().get(uid);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		delegate.get().delete(uid);
	}

	@Override
	public boolean exists(String uid) throws ServerFault {
		return delegate.get().exists(uid);
	}

	@Override
	public int getPriority() {
		return 1;
	}

}
