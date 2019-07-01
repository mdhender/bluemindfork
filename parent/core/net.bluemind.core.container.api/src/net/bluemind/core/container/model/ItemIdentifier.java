/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.core.container.model;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class ItemIdentifier extends ItemVersion {

	public String uid;

	public ItemIdentifier() {
		this(null, 0L, 0L);
	}

	public ItemIdentifier(String uid, long id, long version) {
		super(id, version);
		this.uid = uid;
	}

	public static ItemIdentifier of(String uid, long id, long version) {
		return new ItemIdentifier(uid, id, version);
	}

}
