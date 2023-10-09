/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.imap.endpoint.locks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.vertx.core.Context;
import net.bluemind.imap.endpoint.EndpointRuntimeException;
import net.bluemind.imap.endpoint.driver.SelectedFolder;

public class MailboxSequenceLocks {
	public interface OpCompletionListener {

		public static final OpCompletionListener NOOP = () -> {
		};

		void complete();
	}

	public interface MailboxSeqLock {
		public static final MailboxSeqLock NOOP = new MailboxSeqLock() {

			@Override
			public CompletableFuture<OpCompletionListener> withWriteLock(Context vertx, Object grabber) {
				return CompletableFuture.completedFuture(OpCompletionListener.NOOP);
			}

			@Override
			public CompletableFuture<OpCompletionListener> withReadLock(Context vertx, Object grabber) {
				return CompletableFuture.completedFuture(OpCompletionListener.NOOP);
			}
		};

		CompletableFuture<OpCompletionListener> withReadLock(Context vertx, Object grabber);

		CompletableFuture<OpCompletionListener> withWriteLock(Context vertx, Object grabber);
	}

	private static final Cache<String, MailboxSeqLock> locks = Caffeine.newBuilder()
			.expireAfterAccess(20, TimeUnit.MINUTES).build();

	public static MailboxSeqLock forMailbox(SelectedFolder sel) {
		if (sel == null) {
			SeqLockImpl.logger.warn("Noop locks for {} folder", sel);
			return MailboxSeqLock.NOOP;
		}
		return forMailbox(sel.mailbox.owner.uid);
	}

	public static MailboxSeqLock forMailbox(String owner) {
		return locks.get(owner, SeqLockImpl::new);

	}

	private static final class SeqLockImpl implements MailboxSeqLock {

		private static final Logger logger = LoggerFactory.getLogger(SeqLockImpl.class);
		private static final ScheduledExecutorService REPORT = Executors.newScheduledThreadPool(1);
		private final ReadWriteLock lock;
		private final String owner;

		public SeqLockImpl(String owner) {
			this.owner = owner;
			this.lock = new StampedLock().asReadWriteLock();
		}

		@Override
		public CompletableFuture<OpCompletionListener> withWriteLock(Context vertx, Object grabber) {
			return withLock(lock.writeLock(), vertx, grabber);
		}

		@Override
		public CompletableFuture<OpCompletionListener> withReadLock(Context vertx, Object grabber) {
			return withLock(lock.readLock(), vertx, grabber);
		}

		private CompletableFuture<OpCompletionListener> withLock(Lock l, Context vertx, Object grabber) {
			CompletableFuture<OpCompletionListener> ret = new CompletableFuture<>();
			lockInContext(0, l, vertx, ret, grabber);
			return ret;
		}

		private void lockInContext(int attempt, Lock l, Context vertxContext,
				CompletableFuture<OpCompletionListener> ret, Object grabber) {
			vertxContext.runOnContext(v -> {
				try {
					boolean locked = l.tryLock(20, TimeUnit.MILLISECONDS);
					if (locked) {
						ScheduledFuture<Void> unlockError = REPORT.schedule(() -> {
							logger.error("{} dit not unlock {} in time for {}", owner, l, grabber);
							return null;
						}, 1, TimeUnit.SECONDS);
						ret.complete(() -> {
							unlockError.cancel(false);
							l.unlock();
							logger.debug("[{}] Release lock {} for {} (try: {})", owner, l, grabber, attempt);
						});
					} else {
						logger.warn("[{}] Could not grab lock {} for {} (try: {})", owner, l, grabber, attempt);
						// recurse
						if (attempt > 100) {
							ret.completeExceptionally(new EndpointRuntimeException("Could not grab lock " + l));
							return;
						}
						vertxContext.runOnContext(w -> lockInContext(attempt + 1, l, vertxContext, ret, grabber));
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
		}
	}

}
