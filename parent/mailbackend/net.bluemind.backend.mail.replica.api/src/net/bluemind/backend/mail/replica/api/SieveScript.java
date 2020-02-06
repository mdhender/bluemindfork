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

import io.vertx.core.json.JsonObject;
import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SieveScript {

	public String userId;
	public String fileName;
	public long lastUpdate;
	public boolean isActive;

	public SieveScript() {
	}

	public SieveScript(String uid, String fn, long lastUpdate, boolean isActive) {
		this.userId = uid;
		this.fileName = fn;
		this.lastUpdate = lastUpdate;
		this.isActive = isActive;
	}

	public static SieveScript of(JsonObject js) {
		return new SieveScript(js.getString("USERID"), js.getString("FILENAME"),
				Long.parseLong(js.getString("LAST_UPDATE", "0")), "1".equals(js.getString("ISACTIVE", "0")));
	}

	public String toParenObjectString() {
		return "%(FILENAME " + fileName + " LAST_UPDATE " + lastUpdate + " ISACTIVE " + (isActive ? 1 : 0) + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
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
		SieveScript other = (SieveScript) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

}
