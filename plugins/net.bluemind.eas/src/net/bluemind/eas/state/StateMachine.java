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
package net.bluemind.eas.state;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.HierarchyNode;
import net.bluemind.eas.dto.sync.SyncState;
import net.bluemind.eas.dto.type.ItemDataType;
import net.bluemind.eas.exception.CollectionNotFoundException;
import net.bluemind.eas.store.ISyncStorage;

public class StateMachine {

	protected static final Logger logger = LoggerFactory.getLogger(StateMachine.class);

	private static final String SYNCKEY_VERSION = "4_1";

	private ISyncStorage store;

	public StateMachine(ISyncStorage store) {
		this.store = store;
	}

	/**
	 * @param bs
	 * @param syncKey
	 * @return
	 */
	public SyncState getFolderSyncState(BackendSession bs, String syncKey) {
		if ("0".equals(syncKey)) {
			SyncState ret = new SyncState();
			ret.type = ItemDataType.FOLDER;
			return ret;
		}
		if (store.needReset(bs)) {
			logger.info("reset folder hierarchy for {}'s device {}", bs.getLoginAtDomain(),
					bs.getDeviceId().getIdentifier());
			return null;
		}

		return toSyncState(syncKey);

	}

	/**
	 * @param folderId
	 * @param syncKey
	 * @return
	 * @throws CollectionNotFoundException
	 */
	public SyncState getSyncState(BackendSession bs, int folderId, String syncKey) throws CollectionNotFoundException {

		if (logger.isDebugEnabled()) {
			logger.debug("SyncKey is {}", syncKey);
		}

		if ("0".equals(syncKey)) {
			HierarchyNode folder = store.getHierarchyNode(bs, folderId);
			SyncState ret = new SyncState();
			ret.type = ItemDataType.getValue(folder.containerType);
			return ret;
		}

		return toSyncState(syncKey);
	}

	private SyncState toSyncState(String syncKey) {
		Iterator<String> sss = Splitter.on("-").split(syncKey).iterator();

		String syncKeyVersion = sss.next();
		if (!SYNCKEY_VERSION.equals(syncKeyVersion)) {
			logger.warn("SyncKey '{}' version mismatch. Expected: '{}', was '{}'", syncKey, SYNCKEY_VERSION,
					syncKeyVersion);
			return null;
		}

		SyncState ret = new SyncState();
		ret.date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(sss.next())), ZoneId.systemDefault());
		ret.type = ItemDataType.valueOf(sss.next());
		ret.version = Long.parseLong(sss.next());
		ret.subscriptionVersion = Long.parseLong(sss.next());

		return ret;
	}

	/**
	 * @param type
	 * @return
	 */
	public String generateSyncKey(ItemDataType type) {
		return generateSyncKey(type, 0);
	}

	public String generateSyncKey(ItemDataType type, long version) {

		StringBuilder sk = new StringBuilder(64);

		sk.append(SYNCKEY_VERSION);
		sk.append("-");
		sk.append(System.currentTimeMillis());
		sk.append("-");
		sk.append(type.asXmlValue());
		sk.append("-");
		sk.append(version);
		sk.append("-0");

		return sk.toString();
	}

	public static long extractTimestamp(String syncKey) {
		Iterator<String> iter = Splitter.on("-").split(syncKey).iterator();
		iter.next();
		return Long.valueOf(iter.next());
	}

	public String generateSyncKey(ItemDataType type, long version, long subscriptionVersion) {

		StringBuilder sk = new StringBuilder(64);

		sk.append(SYNCKEY_VERSION);
		sk.append("-");
		sk.append(System.currentTimeMillis());
		sk.append("-");
		sk.append(type.asXmlValue());
		sk.append("-");
		sk.append(version);
		sk.append("-");
		sk.append(subscriptionVersion);

		return sk.toString();
	}
}
