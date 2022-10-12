/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.metrics.transport;

import java.util.concurrent.TimeUnit;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.stream.Field;

import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;

import net.bluemind.delivery.lmtp.common.LmtpEnvelope;
import net.bluemind.delivery.lmtp.filters.FilterException;
import net.bluemind.delivery.lmtp.filters.ILmtpFilterFactory;
import net.bluemind.delivery.lmtp.filters.IMessageFilter;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;

public class TransportLatencyFilter implements IMessageFilter {

	private final Registry registry;
	private final IdFactory idFactory;

	public static class Factory implements ILmtpFilterFactory {

		private final Registry registry;
		private final IdFactory idFactory;

		public Factory() {
			this.registry = MetricsRegistry.get();
			this.idFactory = new IdFactory("traffic", registry, TransportLatencyFilter.class);
		}

		@Override
		public int getPriority() {
			// run last to account for other filter's induced latency
			return Integer.MIN_VALUE;
		}

		@Override
		public IMessageFilter getEngine() {
			return new TransportLatencyFilter(registry, idFactory);
		}

	}

	public TransportLatencyFilter(Registry registry, IdFactory idFactory) {
		this.registry = registry;
		this.idFactory = idFactory;
	}

	@Override
	public Message filter(LmtpEnvelope env, Message message) throws FilterException {
		Field firstHopTimestamp = message.getHeader().getField("X-Bm-Transport-Timestamp");
		if (firstHopTimestamp != null) {
			long firstHopTime = Long.parseLong(firstHopTimestamp.getBody());
			long currentTime = MQ.clusterTime();
			long latencyMs = currentTime - firstHopTime;
			Timer timer = registry.timer(idFactory.name("transportLatency"));
			timer.record(latencyMs, TimeUnit.MILLISECONDS);
		}
		return null;
	}

}
