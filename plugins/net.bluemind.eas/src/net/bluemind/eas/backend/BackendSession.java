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
package net.bluemind.eas.backend;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.http.HttpServerRequest;

import com.google.common.collect.ImmutableMap;

import net.bluemind.eas.dto.IPreviousRequestsKnowledge;
import net.bluemind.eas.dto.device.DeviceId;
import net.bluemind.eas.dto.sync.CollectionSyncRequest;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.user.MSUser;

public class BackendSession implements IPreviousRequestsKnowledge {

	private static final Logger logger = LoggerFactory.getLogger(BackendSession.class);

	private static final boolean isMultiCalForced = new File(System.getProperty("user.home") + "/eas.multical")
			.exists();

	private final ImmutableMap<String, String> hints;
	private final MSUser user;
	private final DeviceId deviceId;
	private final double protocolVersion;

	// persistent state, to keep between requests
	private SessionPersistentState persistentState;
	private HttpServerRequest request;
	private Object internalState;

	public BackendSession(MSUser user, DeviceId device, double protocolVersion) {
		this.user = user;
		this.protocolVersion = protocolVersion;
		this.deviceId = device;
		this.hints = loadHints();

	}

	public boolean checkHint(String key, boolean defaultValue) {
		if (!hints.containsKey(key)) {
			return defaultValue;
		}
		return "true".equals(hints.get(key));
	}

	private ImmutableMap<String, String> loadHints() {
		Properties hints = new Properties();
		try (InputStream in = BackendSession.class.getClassLoader()
				.getResourceAsStream("hints/" + deviceId.getType() + ".hints")) {
			hints.load(in);
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded hints for {}", deviceId.getType());
			}
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("could not load hints for device type {}", deviceId.getType(), e);
			}
		}
		// we copy as we don't want a synchronized data structure
		Map<String, String> hash = new HashMap<>();
		for (Entry<Object, Object> entry : hints.entrySet()) {
			hash.put(entry.getKey().toString(), entry.getValue().toString());
		}
		return ImmutableMap.copyOf(hash);
	}

	public String getLoginAtDomain() {
		return user.getLoginAtDomain();
	}

	public String getSid() {
		return user.getSid();
	}

	public String getDevId() {
		return deviceId.getIdentifier();
	}

	public double getProtocolVersion() {
		return protocolVersion;
	}

	public void setPolicyKey(String pKey) {
		this.persistentState.setPolicyKey(pKey);
	}

	public String getPolicyKey() {
		return persistentState.getPolicyKey();
	}

	public Set<CollectionSyncRequest> getLastMonitored() {
		return persistentState.getLastMonitored();
	}

	public void setLastMonitored(Set<CollectionSyncRequest> lastMonitored) {
		persistentState.setLastMonitored(lastMonitored);
	}

	public Queue<ItemChangeReference> getUnSynchronizedItemChange(Integer collectionId) {
		return persistentState.getUnSynchronizedItemChangeByCollection().computeIfAbsent(collectionId,
				col -> new LinkedBlockingQueue<ItemChangeReference>());
	}

	public void addLastClientSyncState(Integer collectionId, SyncState synckey) {
		persistentState.getLastClientSyncState().put(collectionId, synckey);
	}

	public void clearAll() {
		persistentState.setUpdatedSyncDate(new HashMap<Integer, Date>());
		persistentState.setUnSynchronizedItemChangeByCollection(new HashMap<Integer, Queue<ItemChangeReference>>());
		persistentState.setLastClientSyncState(new HashMap<Integer, SyncState>());
	}

	public void clear(Integer collectionId) {
		persistentState.getUpdatedSyncDate().remove(collectionId);
		persistentState.getUnSynchronizedItemChangeByCollection().remove(collectionId);
		persistentState.getLastClientSyncState().remove(collectionId);
	}

	public void setLastWaitSeconds(Integer lastWait) {
		persistentState.setLastWait(lastWait);
	}

	public void setHeartbeart(Long heartbeat) {
		persistentState.setHeartbeat(heartbeat);
	}

	public Long getHeartbeart() {
		return persistentState.getHeartbeat();
	}

	public MSUser getUser() {
		return user;
	}

	public DeviceId getDeviceId() {
		return deviceId;
	}

	public String getLang() {
		return user.getLang();
	}

	public boolean isMultiCal() {
		return checkHint("hint.multiCalendars", false) || isMultiCalForced;
	}

	public boolean isMultiAB() {
		return checkHint("hint.multiAddressbooks", false);
	}

	public void setMutableState(SessionPersistentState mutableState) {
		this.persistentState = mutableState;
	}

	public HttpServerRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServerRequest request) {
		this.request = request;
	}

	@SuppressWarnings("unchecked")
	public <T> T getInternalState() {
		return (T) internalState;
	}

	public void setInternalState(Object internalState) {
		this.internalState = internalState;
	}

}
