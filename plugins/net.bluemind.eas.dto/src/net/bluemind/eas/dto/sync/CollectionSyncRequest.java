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
package net.bluemind.eas.dto.sync;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.CollectionItem;

public class CollectionSyncRequest {

	private final List<CollectionItem> fetchIds;
	private final List<CollectionItem> deletedIds;
	private final List<Element> changedItems;
	private final List<Element> createdItems;
	private String dataClass;
	private CollectionId collectionId;
	private String syncKey;
	private Integer truncation;
	private boolean deletesAsMoves;
	private Integer windowSize;
	private Boolean getChanges;

	public static class Options {

		public static enum ConflicResolution {
			CLIENT_WINS, // 0
			SERVER_WINS; // 1

			public static ConflicResolution fromXml(String txt) {
				return Integer.parseInt(txt) == 0 ? CLIENT_WINS : SERVER_WINS;
			}

		}

		public BodyOptions bodyOptions;
		public FilterType filterType;

		/**
		 * If the Conflict element is not present, the server object will replace the
		 * client object when a conflict occurs.
		 * 
		 * A value of 0 (zero) means to keep the client object; a value of 1 means to
		 * keep the server object. If the value is 1 and there is a conflict, a Status
		 * element (section 2.2.3.166.16) value of 7 is returned to inform the client
		 * that the object that the client sent to the server was discarded.
		 */
		public ConflicResolution conflictPolicy = ConflicResolution.SERVER_WINS;

	}

	public Options options;

	public int fetched = 0;
	public int addedAndUpdated = 0;
	public boolean forceResponse = false;

	public CollectionSyncRequest() {
		fetchIds = new LinkedList<>();
		deletedIds = new LinkedList<>();
		changedItems = new LinkedList<>();
		createdItems = new LinkedList<>();
		truncation = 9; // FIXME use enum SyncHandler.SYNC_TRUNCATION_ALL;
		windowSize = 25;
	}

	public String getDataClass() {
		return dataClass;
	}

	public void setDataClass(String dataClass) {
		this.dataClass = dataClass;
	}

	public CollectionId getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(CollectionId collectionId) {
		this.collectionId = collectionId;
	}

	public String getSyncKey() {
		return syncKey;
	}

	public void setSyncKey(String syncKey) {
		this.syncKey = syncKey;
	}

	public Integer getTruncation() {
		return truncation;
	}

	public void setTruncation(Integer truncation) {
		this.truncation = truncation;
	}

	public boolean isDeletesAsMoves() {
		return deletesAsMoves;
	}

	public void setDeletesAsMoves(boolean deletesAsMoves) {
		this.deletesAsMoves = deletesAsMoves;
	}

	public Collection<CollectionItem> getFetchIds() {
		return fetchIds;
	}

	public Integer getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(Integer windowSize) {
		this.windowSize = windowSize;
	}

	@Override
	public boolean equals(Object obj) {
		return collectionId.equals(collectionId);
	}

	@Override
	public int hashCode() {
		return collectionId.hashCode();
	}

	public Collection<CollectionItem> getDeletedIds() {
		return deletedIds;
	}

	public Collection<Element> getChangedItems() {
		return changedItems;
	}

	public Collection<Element> getCreatedItems() {
		return createdItems;
	}

	public Boolean isGetChanges() {
		return getChanges;
	}

	public void setGetChanges(boolean getChanges) {
		this.getChanges = getChanges;
	}

}
