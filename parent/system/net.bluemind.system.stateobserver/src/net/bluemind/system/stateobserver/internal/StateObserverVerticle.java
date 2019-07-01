/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.stateobserver.internal;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Registry;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.rest.http.HttpClientProvider;
import net.bluemind.core.rest.http.ILocator;
import net.bluemind.core.rest.http.VertxPromiseServiceProvider;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.metrics.registry.IdFactory;
import net.bluemind.metrics.registry.MetricsRegistry;
import net.bluemind.network.topology.IServiceTopology;
import net.bluemind.network.topology.Topology;
import net.bluemind.network.topology.TopologyException;
import net.bluemind.system.api.IInstallationPromise;
import net.bluemind.system.api.SystemState;
import net.bluemind.system.stateobserver.IStateListener;

public class StateObserverVerticle extends Verticle {

	private static final Logger logger = LoggerFactory.getLogger(StateObserverVerticle.class);

	private VertxPromiseServiceProvider coreProvider;
	private SystemState state;

	private long lastUpdate;

	private Registry reg;
	private IdFactory metricsId;
	private Gauge ageGauge;
	private Counter failuresCounter;

	private static enum StateUpdateOrigin {

		/**
		 * We asked to core with an http request and got an answer
		 */
		DIRECT_FETCH_SUCCESS,

		/**
		 * We asked to core with an http request and it failed.
		 */
		DIRECT_FETCH_FAILURE,

		/**
		 * We failed to receive an heatbeat event for too long
		 */
		HEARTBEAT_FAILURE,

		/**
		 * An event bus event (forwarded from hz) triggers a state change
		 */
		BUS_EVENT,

		/**
		 * bm/core left cluster
		 */
		UPDATE_STATE_MEMBERSHIP_ADDRESS

	}

	public StateObserverVerticle() {
	}

	@Override
	public void start() {
		this.reg = MetricsRegistry.get();
		this.metricsId = new IdFactory("heartbeat.receiver", reg, CoreForward.class);
		this.ageGauge = reg.gauge(metricsId.name("age"));
		this.failuresCounter = reg.counter(metricsId.name("failures"));

		HttpClientProvider clientProvider = new HttpClientProvider(vertx);
		ILocator topoLocator = (String service, AsyncHandler<String[]> asyncHandler) -> {
			Optional<IServiceTopology> topo = Topology.getIfAvailable();
			if (topo.isPresent()) {
				String core = topo.get().core().value.address();
				String[] resp = new String[] { core };
				asyncHandler.success(resp);
			} else {
				asyncHandler.failure(new TopologyException("topology not available"));
			}
		};
		coreProvider = new VertxPromiseServiceProvider(clientProvider, topoLocator, null, Collections.emptyList());

		lastUpdate = System.nanoTime();
		vertx.setPeriodic(1000, (h) -> {
			hearbeatCheck();
		});

		vertx.eventBus().registerHandler(Topic.CORE_NOTIFICATIONS, new Handler<Message<JsonObject>>() {
			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject msg = event.body();
				SystemState newState = SystemState.fromOperation(msg.getString("operation"));
				updateState(newState, StateUpdateOrigin.BUS_EVENT);
			}
		}, (v) -> refreshState());

		vertx.eventBus().registerHandler(IStateListener.STATE_BUS_EP_ADDRESS, new Handler<Message<Void>>() {

			@Override
			public void handle(Message<Void> event) {
				event.reply(state.name());
			}
		});

		vertx.eventBus().registerHandler(MQ.MEMBERSHIP_EVENTS_ADDRESS, new Handler<Message<JsonObject>>() {

			@Override
			public void handle(Message<JsonObject> event) {
				JsonObject eventBody = event.body();
				if ("memberRemoved".equals(eventBody.getString("type"))
						&& "bm-core".equals(eventBody.getString("memberKind"))) {

					updateState(SystemState.CORE_STATE_MAINTENANCE, StateUpdateOrigin.UPDATE_STATE_MEMBERSHIP_ADDRESS);
					refreshState();
				}

			}
		});

	}

	private void hearbeatCheck() {
		long sinceHearbeat = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastUpdate);
		ageGauge.set(sinceHearbeat);
		if (sinceHearbeat > 8000) {
			failuresCounter.increment();
			// woop, no message since 8s, go CORE_STATE_UNKOWN
			logger.warn("no heartbeat since {} ms, switch to UNKNOWN & trigger a refresh", sinceHearbeat);
			refreshState();
		}
	}

	private void refreshState() {
		coreProvider.instance(IInstallationPromise.class).getSystemState().thenAccept(coreState -> {
			updateState(coreState, StateUpdateOrigin.DIRECT_FETCH_SUCCESS);
		}).exceptionally(t -> {
			logger.error("error retrieving core state : {}", t.getMessage());
			if (logger.isDebugEnabled()) {
				logger.debug("error retrieving core state", t);
			}

			updateState(SystemState.CORE_STATE_UNKNOWN, StateUpdateOrigin.DIRECT_FETCH_FAILURE);
			vertx.setTimer(1000, l -> refreshState());
			return null;
		});
	}

	private void updateState(SystemState newState, StateUpdateOrigin origin) {
		if (newState == SystemState.CORE_STATE_UNKNOWN && !new File("/etc/bm/mcast.id").exists()) {
			newState = SystemState.CORE_STATE_NOT_INSTALLED;
		}

		lastUpdate = System.nanoTime();
		if (newState != state) {
			logger.info("New core state is {}, cause: {}", newState, origin);
			RunnableExtensionLoader<IStateListener> loader = new RunnableExtensionLoader<IStateListener>();
			List<IStateListener> listeners = loader.loadExtensions("net.bluemind.system", "state", "state-listener",
					"class");
			for (IStateListener listener : listeners) {
				listener.stateChanged(newState);
			}

			vertx.eventBus().publish(IStateListener.STATE_BUS_ADDRESS, newState.name());
		}
		state = newState;
	}

	public SystemState getState() {
		return state;
	}

}
