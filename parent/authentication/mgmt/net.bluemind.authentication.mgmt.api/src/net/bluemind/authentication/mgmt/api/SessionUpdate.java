/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2024
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
package net.bluemind.authentication.mgmt.api;

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class SessionUpdate {

	public List<String> remoteIps;

	public static SessionUpdate forIp(String remoteIp) {
		SessionUpdate su = new SessionUpdate();
		su.remoteIps = Collections.singletonList(remoteIp);
		return su;
	}

}
