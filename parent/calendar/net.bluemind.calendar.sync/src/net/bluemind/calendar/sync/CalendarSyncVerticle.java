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
package net.bluemind.calendar.sync;

import java.sql.SQLException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import net.bluemind.calendar.service.internal.CalendarService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerSyncResult;
import net.bluemind.core.container.model.ContainerSyncStatus;
import net.bluemind.core.container.model.ContainerSyncStatus.Status;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.ContainerSyncStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.NullTaskMonitor;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

/**
 * Synchronize <i>active</i> remote calendars. <i>Healthy</i> ones and
 * <i>waiting</i> ones go first. <br>
 * <ul>
 * <li><i>Active</i> means: when a {@link CalendarService} is created for that
 * calendar</li>
 * <li><i>Healthy</i> means: the less synchronization errors the more
 * healthy</li>
 * <li><i>Waiting</i> means: the number of days since the last
 * synchronization</li>
 * <li>When {@link #syncErrorLimit()} synchronization errors is reached, a
 * calendar is excluded from the synchronization mechanism FIXME how to
 * recover?</li>
 * <li>Each synchronization of a same calendar is delayed by
 * {@link CalendarContainerSync#nextSyncDelay()} milliseconds</li>
 * </ul>
 */
public class CalendarSyncVerticle extends Verticle {
	public static final String EVENT_ADDRESS = "bm.calendar.service.accessed";

	private static final Logger LOGGER = LoggerFactory.getLogger(CalendarSyncVerticle.class);

	/** The synchronization's {@link Executor}. */
	private ThreadPoolExecutor executor;

	/**
	 * Keep tracks of synchronizing calendars. Helps to avoid having more than
	 * one synchronization of the same calendar at a time.
	 */
	private static final Set<String> syncingCals = ConcurrentHashMap.newKeySet();

	/**
	 * A calendar synchronization with too many changes will be considered as
	 * erroneous.
	 */
	private static final int MAX_SYNC_OPERATIONS = 50;

	/** When this limit is reached, sync on demand stops. */
	public static int syncErrorLimit() {
		return CalendarService.SYNC_ERRORS_LIMIT;
	}

	@Override
	public void start() {
		this.vertx.eventBus().registerHandler(EVENT_ADDRESS, this::queue);
		this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
				new PriorityBlockingQueue<Runnable>(256, this::queueComparator));
	}

	private int queueComparator(final Runnable r1, final Runnable r2) {
		return this.syncStatusComparator((ContainerSyncStatus) r1, (ContainerSyncStatus) r2);
	}

	/**
	 * <i>Note: <code>public</code> access is needed by tests because of OSGI
	 * runtime behavior. </i>
	 */
	public int syncStatusComparator(final ContainerSyncStatus syncStatus1, final ContainerSyncStatus syncStatus2) {
		return weight(syncStatus1) - weight(syncStatus2);
	}

	/**
	 * The more the weight the less a calendar's synchronization is
	 * prioritized.<br>
	 * 
	 * <pre>
	 *  100 x syncErrors - daysSinceLastSync
	 * </pre>
	 * 
	 * <i>Note: this method does not support
	 * {@link ContainerSyncStatus#errors}<code>/100</code> equals or greater
	 * than {@link Integer#MAX_VALUE}. Results are then unusable.</i>
	 */
	private static int weight(final ContainerSyncStatus containerSyncStatus) {
		return 100 * containerSyncStatus.errors - daysSinceLastSync(containerSyncStatus);
	}

	/**
	 * If conditions are met, add a calendar to the synchronization
	 * executor.<br>
	 * <i>Note: this method is a {@link Handler}'s callback.</i>
	 * 
	 * @param message
	 *            the {@link Message} given by the {@link EventBus}
	 */
	private void queue(final Message<JsonObject> message) {
		final JsonObject messageBody = message.body();
		final String calendarUid = messageBody.getString("calendarUid");
		final String origin = messageBody.getString("origin");
		final boolean hasExternalOrigin = !SecurityContext.SYSTEM.getOrigin().equals(origin);
		final boolean isRemote = messageBody.getBoolean("isRemote");
		final RunnableSyncStatus syncStatus = new RunnableSyncStatus(calendarUid);
		// in order to be eligible for synchronization, a calendar must meet
		// these conditions:
		// 1) be remote
		// 2) have less than SYNC_ERRORS_LIMIT synchronization errors
		// 3) having reached the delay between two synchronizations
		// 4) not being currently queued for synchronization
		// 5) the "bm.calendar.service.accessed" event must have an "external"
		// origin
		// for performance matters, these conditions are split in the following
		// code:
		try {
			if (isRemote && hasExternalOrigin && !syncingCals.contains(syncStatus.id)) {
				final BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
				final ContainerStore containerStore = new ContainerStore(context,
						DataSourceRouter.get(context, calendarUid), context.getSecurityContext());
				final ContainerSyncStore containerSyncStore = new ContainerSyncStore(
						DataSourceRouter.get(context, calendarUid), containerStore.get(calendarUid));
				syncStatus.load(containerSyncStore.getSyncStatus());
				if (syncStatus.errors < syncErrorLimit() && System.currentTimeMillis() > syncStatus.nextSync) {
					syncingCals.add(syncStatus.id);
					this.executor.execute(syncStatus);
				}
			}
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	/**
	 * Retrieve changes of the remote calendar of higher priority then update
	 * our internal version.
	 */
	private static void synchronize(final RunnableSyncStatus syncStatus) {
		// prepare context, container...
		final BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		final ContainerStore containerStore = new ContainerStore(context, DataSourceRouter.get(context, syncStatus.id),
				context.getSecurityContext());
		final Container container;
		try {
			container = containerStore.get(syncStatus.id);
		} catch (SQLException e) {
			LOGGER.error("Unable to retrieve container {}", syncStatus.id, e);
			return;
		}

		// rely on CalendarContainerSync to do the actual synchronization
		try {
			final CalendarContainerSync calendarContainerSync = new CalendarContainerSync(context, container);
			final ContainerSyncResult syncResult = calendarContainerSync.sync(syncStatus.syncTokens,
					new NullTaskMonitor());
			updateSyncStatus(syncStatus, syncResult, context, container);
		} catch (Exception e) {
			LOGGER.error("synchronizeCalendar {} failed", syncStatus.id, e);
		}
	}

	private static void updateSyncStatus(final ContainerSyncStatus previousSyncStatus,
			final ContainerSyncResult containerSyncResult, final BmContext context, final Container container) {
		if (hasTooManySyncOperations(containerSyncResult)) {
			containerSyncResult.status.syncStatus = Status.ERROR;
			LOGGER.warn("Calendar {} ICS has too many changes (>{})", container.name, MAX_SYNC_OPERATIONS);
		}

		// update the calendar synchronization status
		final ContainerSyncStatus newStatus;
		if (containerSyncResult == null || Status.ERROR == containerSyncResult.status.syncStatus) {
			// increment errors
			if (containerSyncResult == null) {
				newStatus = previousSyncStatus;
			} else {
				newStatus = containerSyncResult.status;
			}
			newStatus.errors = previousSyncStatus != null ? previousSyncStatus.errors + 1 : 1;
		} else {
			newStatus = containerSyncResult.status;
			// a success resets errors
			newStatus.errors = 0;
		}
		final ContainerSyncStore containerSyncStore = new ContainerSyncStore(
				DataSourceRouter.get(context, container.uid), container);
		containerSyncStore.setSyncStatus(newStatus);
	}

	private static boolean hasTooManySyncOperations(final ContainerSyncResult containerSyncResult) {
		final int operations = containerSyncResult.added + containerSyncResult.removed + containerSyncResult.updated;
		final int daysSinceLastSync = daysSinceLastSync(containerSyncResult.status);
		if (daysSinceLastSync > 0) {
			return operations / daysSinceLastSync(containerSyncResult.status) > MAX_SYNC_OPERATIONS;
		} else {
			return operations > MAX_SYNC_OPERATIONS;
		}
	}

	protected static int daysSinceLastSync(final ContainerSyncStatus containerSyncStatus) {
		final long now = System.currentTimeMillis();
		final long lastSync = containerSyncStatus.lastSync != null ? containerSyncStatus.lastSync.getTime() : 0;
		return (int) TimeUnit.MILLISECONDS.toDays(now - lastSync);
	}

	/**
	 * Convenient class for manipulating {@link ContainerSyncStatus}es from
	 * different containers.
	 */
	private static class RunnableSyncStatus extends ContainerSyncStatus implements Runnable {
		private String id;

		public RunnableSyncStatus(final String containerUid) {
			this.id = containerUid;
		}

		public void load(final ContainerSyncStatus containerSyncStatus) {
			if (containerSyncStatus != null) {
				this.syncTokens = containerSyncStatus.syncTokens;
				this.lastSync = containerSyncStatus.lastSync;
				this.nextSync = containerSyncStatus.nextSync != null ? containerSyncStatus.nextSync : 0;
				this.errors = containerSyncStatus.errors;
			} else {
				// no sync has been done yet, for comparisons sake, set the last
				// and next synchronization to 1970-01-01 (epoch)
				this.lastSync = new Date(0);
				this.nextSync = 0L;
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			RunnableSyncStatus other = (RunnableSyncStatus) obj;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}

		@Override
		public void run() {
			synchronize(this);
			syncingCals.remove(this.id);
		}
	}

	public static class Factory implements IVerticleFactory, IUniqueVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new CalendarSyncVerticle();
		}
	}
}
