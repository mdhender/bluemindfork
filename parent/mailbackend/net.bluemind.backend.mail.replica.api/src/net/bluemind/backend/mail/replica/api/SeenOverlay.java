/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.api;

import com.google.common.base.MoreObjects;

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SeenOverlay {

	public String userId;
	public String uniqueId;
	public long lastRead;
	public long lastUid;
	public long lastChange;
	public String seenUids;

	public static SeenOverlay of(JsonObject js) {
		SeenOverlay so = new SeenOverlay();
		so.userId = js.getString("USERID");
		so.uniqueId = js.getString("UNIQUEID");
		so.lastRead = Long.parseLong(js.getString("LASTREAD"));
		so.lastUid = Long.parseLong(js.getString("LASTUID"));
		so.lastChange = Long.parseLong(js.getString("LASTCHANGE"));
		so.seenUids = js.getString("SEENUIDS");
		return so;

	}

	/**
	 * 
	 * %(UNIQUEID 6bef31d9586a10c9 LASTREAD 1483355254 LASTUID 1 LASTCHANGE
	 * 1483355252 SEENUIDS "")
	 * 
	 * @return
	 */
	public String toParenObjectString() {
		return "%(UNIQUEID " + uniqueId + " LASTREAD " + lastRead + " LASTUID " + lastUid + " LASTCHANGE " + lastChange
				+ " SEENUIDS \"" + seenUids + "\")";
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(SeenOverlay.class)//
				.add("userId", userId).add("mbox", uniqueId)//
				.add("seen", seenUids)//
				.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((uniqueId == null) ? 0 : uniqueId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		SeenOverlay other = (SeenOverlay) obj;
		if (uniqueId == null) {
			if (other.uniqueId != null)
				return false;
		} else if (!uniqueId.equals(other.uniqueId))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

}
