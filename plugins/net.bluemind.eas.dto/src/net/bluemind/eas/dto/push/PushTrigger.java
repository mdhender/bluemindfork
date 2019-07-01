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
package net.bluemind.eas.dto.push;

public class PushTrigger {

	public final int collectionId;
	public final String userUid;
	public final boolean folderSyncRequired;
	public final boolean noChanges;

	private PushTrigger(int collectionId, String userUid, boolean folderSyncRequired, boolean noChanges) {
		this.collectionId = collectionId;
		this.userUid = userUid;
		this.folderSyncRequired = folderSyncRequired;
		this.noChanges = noChanges;
	}

	public static final PushTrigger forCollection(int colId) {
		return new PushTrigger(colId, null, false, false);
	}

	private static final PushTrigger NO_CHANGES = new PushTrigger(0, null, false, true);

	public static final PushTrigger noChanges() {
		return NO_CHANGES;
	}

	public static PushTrigger hierarchyChanged(String owner) {
		return new PushTrigger(0, owner, true, false);
	}

}
