/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.sds.sync.service.internal.queue;

import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Suppliers;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.sds.sync.api.SdsSyncEvent;
import net.bluemind.sds.sync.api.SdsSyncEvent.Body;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;

public class SdsSyncQueue implements AutoCloseable {
	private static final Supplier<String> QUEUES_ROOT = Suppliers
			.memoize(() -> System.getProperty("bm.sdssyncqueue", "/var/cache/bm-core/sds-sync-queue"));
	private static final Logger logger = LoggerFactory.getLogger(SdsSyncQueue.class);
	private static final Set<SdsSyncEvent> BODIES_EVENTS = Set.of(SdsSyncEvent.BODYADD, SdsSyncEvent.BODYDEL);
	private final SingleChronicleQueue queue;

	static {
		System.setProperty("chronicle.disk.monitor.disable", "true");
		System.setProperty("chronicle.analytics.disable", Boolean.TRUE.toString());
		Jvm.setResourceTracing(false);
		AbstractReferenceCounted.disableReferenceTracing();
	}

	public SdsSyncQueue() {
		queue = SingleChronicleQueueBuilder.single(QUEUES_ROOT.get()).build();
	}

	public void putBody(SdsSyncEvent evt, Body body) {
		if (queue.isClosed()) {
			if (logger.isWarnEnabled()) {
				logger.warn("Sds Sync Queue is closed, msg ({}:{}) dropped", evt.busName(), body);
			}
			throw new ServerFault("queue is closed");
		}
		if (!BODIES_EVENTS.contains(evt)) {
			throw new ServerFault("Programming error: evt " + evt + " is not BODYADD|BODYDEL");
		}

		try (ExcerptAppender appender = queue.acquireAppender()) {
			appender.writeDocument(w -> w.write("sdssync") //
					.marshallable(m -> m.write("type").text(evt.name()) //
							.write("key").bytes(body.guid()) //
							.write("srv").text(body.serverUid())));
		}
	}

	public void putFileHosting(SdsSyncEvent evt, String key) {
		if (queue.isClosed()) {
			if (logger.isWarnEnabled()) {
				logger.warn("Sds Sync Queue is closed, msg ({}:{}) dropped", evt.busName(), key);
			}
			throw new ServerFault("queue is closed");
		}
		if (!Set.of(SdsSyncEvent.FHADD).contains(evt)) {
			throw new ServerFault("Programming error: evt " + evt + " is not FHADD");
		}

		try (ExcerptAppender appender = queue.acquireAppender()) {
			appender.writeDocument(w -> w.write("sdssync") //
					.marshallable(m -> m.write("type").text(evt.name()) //
							.write("key").text(key)));
		}
	}

	@Override
	public void close() throws Exception {
		queue.close();
	}

	public ExcerptTailer createTailer() {
		return queue.createTailer();
	}

	public SingleChronicleQueue queue() {
		return queue;
	}
}
