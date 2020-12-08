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
package net.bluemind.eas.dto.base;

import net.bluemind.eas.dto.sync.CollectionId;

/**
 * An immutable object use to represent stable server side identifiers like
 * <code>123:45</code>
 *
 */
public final class CollectionItem {

	public final CollectionId collectionId;
	public final long itemId;

	/**
	 * Creates a collection item from the unique identifier of an item in a
	 * collection like <code>123:45</code> where 123 is the collection id and 45 is
	 * the item id in the collection.
	 * 
	 * @param colAndServer a server uid string like
	 * 
	 * @return an immutable collection item
	 */
	public static CollectionItem of(String colAndServer) {
		int idx = colAndServer.indexOf(':');
		if (idx <= 0) {
			throw new RuntimeException("Invalid server id: '" + colAndServer + "'");
		}
		return of(colAndServer.substring(0, idx), Long.valueOf(colAndServer.substring(idx + 1)));
	}

	public String toString() {
		return String.format("%s:%d", collectionId.getValue(), itemId);
	}

	private CollectionItem(CollectionId collectionId, long itemId) {
		this.collectionId = collectionId;
		this.itemId = itemId;
	}

	public static CollectionItem of(String collectionId, long itemId) {
		return of(CollectionId.of(collectionId), itemId);
	}

	public static CollectionItem of(CollectionId collectionId, long itemId) {
		return new CollectionItem(collectionId, itemId);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((collectionId == null) ? 0 : collectionId.hashCode());
		result = prime * result + (int) (itemId ^ (itemId >>> 32));
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
		CollectionItem other = (CollectionItem) obj;
		if (collectionId == null) {
			if (other.collectionId != null)
				return false;
		} else if (!collectionId.equals(other.collectionId))
			return false;
		if (itemId != other.itemId)
			return false;
		return true;
	}

}
