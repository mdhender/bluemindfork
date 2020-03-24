/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.eas.dto.sync;

import java.util.Iterator;
import java.util.Optional;

import com.google.common.base.Splitter;

public class CollectionId {

	public static final String SEPARATOR = "__";
	public static final Splitter splitter = Splitter.on(SEPARATOR);

	private Optional<Long> subscriptionId;
	private int folderId;

	private CollectionId(Optional<Long> subscriptionId, int folderId) {
		this.subscriptionId = subscriptionId;
		this.folderId = folderId;
	}

	public static CollectionId of(long subscriptionId, String folderId) {
		return new CollectionId(subscriptionId > 0 ? Optional.of(subscriptionId) : Optional.empty(),
				Integer.parseInt(folderId));
	}

	public static CollectionId of(String collectionId) {
		Optional<Long> subscriptionId = Optional.empty();
		String folderId = collectionId;
		if (folderId.contains(SEPARATOR)) {
			Iterator<String> colIdIterator = splitter.split(folderId).iterator();
			subscriptionId = Optional.of(Long.parseLong(colIdIterator.next()));
			folderId = colIdIterator.next();
		}

		return new CollectionId(subscriptionId, Integer.parseInt(folderId));
	}

	public Optional<Long> getSubscriptionId() {
		return subscriptionId;
	}

	public int getFolderId() {
		return folderId;
	}

	public String getValue() {
		if (!subscriptionId.isPresent()) {
			return Integer.toString(folderId);
		}

		return subscriptionId.get() + SEPARATOR + folderId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + folderId;
		result = prime * result + ((subscriptionId == null) ? 0 : subscriptionId.hashCode());
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
		CollectionId other = (CollectionId) obj;
		if (folderId != other.folderId)
			return false;
		if (subscriptionId == null) {
			if (other.subscriptionId != null)
				return false;
		} else if (!subscriptionId.equals(other.subscriptionId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CollectionId [subscriptionId=" + subscriptionId + ", folderId=" + folderId + "]";
	}

}
