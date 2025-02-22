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
package net.bluemind.imap.vertx.cmd;

import com.google.common.base.MoreObjects;

public class AppendResponse {

	/**
	 * This is > 0 if the append succeeded or -1 otherwise.
	 */
	public final long newUid;

	/**
	 * This is not null when the append failed
	 */
	public final String reason;

	public AppendResponse(long newUid) {
		this.newUid = newUid;
		this.reason = null;
	}

	public AppendResponse(String reason) {
		this.newUid = -1;
		this.reason = reason;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(AppendResponse.class).add("uid", newUid).add("reason", reason).toString();
	}

}
