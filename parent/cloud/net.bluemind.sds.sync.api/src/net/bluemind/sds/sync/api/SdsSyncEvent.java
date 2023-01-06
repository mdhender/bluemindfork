/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.sds.sync.api;

import io.vertx.core.json.JsonObject;

/* EventBus events */
public enum SdsSyncEvent {
	/* Message body added to the store */
	BODYADD("sds.sync.body.add"), //

	/* Message body removed from the store */
	BODYDEL("sds.sync.body.del"), //

	/* File added to fileHosting */
	FHADD("sds.sync.fh.add");

	private String eventbusName;

	private SdsSyncEvent(String eventbusName) {
		this.eventbusName = eventbusName;
	}

	public String busName() {
		return eventbusName;
	}

	public static record Body(byte[] guid, String serverUid) {
		public JsonObject toJson() {
			return new JsonObject().put("guid", guid).put("serverUid", serverUid);
		}

		public static Body fromJson(JsonObject jo) {
			return new Body(jo.getBinary("guid"), jo.getString("serverUid"));
		}
	}
}