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
package net.bluemind.eas.command.sync;

import java.util.Collections;
import java.util.List;

import net.bluemind.eas.dto.sync.CollectionSyncResponse.ServerChange;
import net.bluemind.eas.dto.sync.SyncStatus;

public class CollectionChanges {

	public List<ServerChange> commands = Collections.emptyList();
	public String syncKey;
	public boolean moreAvailable;
	public SyncStatus status = SyncStatus.OK;

}
