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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.bluemind.eas.dto.sync.CollectionSyncRequest;
import net.bluemind.eas.dto.sync.SyncState;

public class SessionPersistentState {
	private Map<Integer, Date> updatedSyncDate;
	private Map<Integer, Queue<ItemChangeReference>> unSynchronizedItemChangeByCollection;
	private Map<Integer, SyncState> lastClientSyncState;
	private Integer lastWaitSeconds;
	private String policyKey;
	private Set<CollectionSyncRequest> lastMonitored;
	private Long heartbeat;

	public SessionPersistentState() {
		unSynchronizedItemChangeByCollection = new HashMap<Integer, Queue<ItemChangeReference>>();
		lastClientSyncState = new HashMap<Integer, SyncState>();
		updatedSyncDate = new HashMap<Integer, Date>();
		lastMonitored = new HashSet<CollectionSyncRequest>();
	}

	public Map<Integer, Date> getUpdatedSyncDate() {
		return updatedSyncDate;
	}

	public void setUpdatedSyncDate(Map<Integer, Date> updatedSyncDate) {
		this.updatedSyncDate = updatedSyncDate;
	}

	public Map<Integer, Queue<ItemChangeReference>> getUnSynchronizedItemChangeByCollection() {
		return unSynchronizedItemChangeByCollection;
	}

	public void setUnSynchronizedItemChangeByCollection(
			Map<Integer, Queue<ItemChangeReference>> unSynchronizedItemChangeByCollection) {
		this.unSynchronizedItemChangeByCollection = unSynchronizedItemChangeByCollection;
	}

	public Map<Integer, SyncState> getLastClientSyncState() {
		return lastClientSyncState;
	}

	public void setLastClientSyncState(Map<Integer, SyncState> lastClientSyncState) {
		this.lastClientSyncState = lastClientSyncState;
	}

	public Integer getLastWait() {
		return lastWaitSeconds;
	}

	public void setLastWait(Integer lastWait) {
		this.lastWaitSeconds = lastWait;
	}

	public String getPolicyKey() {
		return policyKey;
	}

	public void setPolicyKey(String policyKey) {
		this.policyKey = policyKey;
	}

	public Set<CollectionSyncRequest> getLastMonitored() {
		return lastMonitored;
	}

	public void setLastMonitored(Set<CollectionSyncRequest> lastMonitored) {
		this.lastMonitored = lastMonitored;
	}

	public Long getHeartbeat() {
		return heartbeat;
	}

	public void setHeartbeat(Long heartbeat) {
		this.heartbeat = heartbeat;
	}

}
