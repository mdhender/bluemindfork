/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.core.backup.continuous.restore.domains.replication;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import net.bluemind.backend.cyrus.replication.client.SyncClientOIO;
import net.bluemind.config.Token;
import stormpot.Allocator;
import stormpot.Expiration;
import stormpot.Pool;
import stormpot.Poolable;
import stormpot.Slot;
import stormpot.SlotInfo;
import stormpot.Timeout;

public class SyncClientPools {

	private SyncClientPools() {

	}

	private static final Map<String, Pool<PooledSyncClient>> pools = new ConcurrentHashMap<>();
	private static final Timeout to = new Timeout(5, TimeUnit.SECONDS);

	public static PooledSyncClient getClient(String serverIp, int port) {
		try {
			return pools.computeIfAbsent(serverIp, srv -> createPool(srv, port)).claim(to);
		} catch (Exception e) {
			throw new PoolIssue(e);
		}
	}

	@SuppressWarnings("serial")
	private static class PoolIssue extends RuntimeException {

		public PoolIssue(Exception e) {
			super(e);
		}

	}

	public static class TimedOrClosedExpiration implements Expiration<PooledSyncClient> {
		private Expiration<PooledSyncClient> delegate = Expiration.after(2, 4, TimeUnit.MINUTES);

		@Override
		public boolean hasExpired(SlotInfo<? extends PooledSyncClient> info) throws Exception {
			return delegate.hasExpired(info) || info.getPoolable().isExpired();
		}

	}

	private static Pool<PooledSyncClient> createPool(String serverIp, int port) {
		SyncClientAllocator alloc = new SyncClientAllocator(serverIp, port);
		return Pool.fromInline(alloc).setSize(4).setExpiration(new TimedOrClosedExpiration()).build();
	}

	private static class PooledSyncClient extends SyncClientOIO implements Poolable, AutoCloseable {

		private Slot slot;

		public PooledSyncClient(Slot slot, String host, int port) throws IOException {
			super(host, port);
			this.slot = slot;
		}

		@Override
		public void release() {
			slot.release(this);
		}

		@Override
		public void close() {
			this.release();
		}

		private void closeImpl() throws IOException {
			super.close();
		}

	}

	public static class SyncClientAllocator implements Allocator<PooledSyncClient> {

		private String serverIp;
		private int port;

		public SyncClientAllocator(String serverIp, int port) {
			this.serverIp = serverIp;
			this.port = port;
		}

		@Override
		public PooledSyncClient allocate(Slot slot) throws Exception {
			PooledSyncClient client = new PooledSyncClient(slot, serverIp, port); // NOSONAR
			client.authenticate("admin0", Token.admin0());
			return client;
		}

		@Override
		public void deallocate(PooledSyncClient poolable) throws Exception {
			poolable.closeImpl();
		}

	}

}
