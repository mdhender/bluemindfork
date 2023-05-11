/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.dataprotect.common.restore;

import java.util.List;

import net.bluemind.core.rest.BmContext;

public abstract class CommonRestoreEntities {

	public RestoreRestorableItem item;
	public BmContext back;
	public BmContext live;

	protected CommonRestoreEntities(RestoreRestorableItem item, BmContext back, BmContext live) {
		this.item = item;
		this.back = back;
		this.live = live;
	}

	public abstract void restoreEntities(List<String> allUids, String backUid, String liveUid);

	public abstract void restoreEntities(List<String> allUids);
}
