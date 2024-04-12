/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.eas.http.tests.helpers;

import java.util.Arrays;

import org.w3c.dom.Document;

public record SyncRequest(boolean getChanges, Document[] clientChangesAdd) {

	public static class SyncRequestBuilder {
		boolean getChanges = false;
		Document[] clientChangesAdd = new Document[0];

		public SyncRequestBuilder withChanges() {
			getChanges = true;
			return this;
		}

		public SyncRequestBuilder withClientChangesAdd(Document... clientChanges) {
			this.clientChangesAdd = clientChanges;
			return this;
		}

		public SyncRequest build() {
			return new SyncRequest(getChanges, clientChangesAdd);
		}
	}

	public SyncRequestBuilder copy() {
		SyncRequestBuilder syncRequestBuilder = new SyncRequestBuilder();
		if (getChanges) {
			syncRequestBuilder.withChanges();
		}
		syncRequestBuilder.clientChangesAdd = clientChangesAdd;
		return syncRequestBuilder;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(clientChangesAdd);
		result = prime * result + (getChanges ? 1231 : 1237);
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
		SyncRequest other = (SyncRequest) obj;
		if (!Arrays.equals(clientChangesAdd, other.clientChangesAdd))
			return false;
		if (getChanges != other.getChanges)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getName();
	}

}
