/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.cti.wazo.api.client;

import io.vertx.core.json.JsonObject;
import net.bluemind.cti.wazo.api.client.connection.HttpsWazoApiConnection;
import net.bluemind.cti.wazo.config.WazoEndpoints;
import net.bluemind.user.api.UserAccountInfo;

public class WazoClickToCallClient extends WazoAuthentifiedApiClient {

	public WazoClickToCallClient(String domainUid, UserAccountInfo userAccountInfo) {
		super(domainUid, userAccountInfo);
	}

	public void dial(String number) {
		calld(number, false);
	}

	private void calld(String extension, boolean fromMobile) {

		JsonObject payload = new JsonObject();
		payload.put("all_lines", false);
		payload.put("auto_answer_caller", true);
		payload.put("extension", extension);
		payload.put("from_mobile", fromMobile);

		try (HttpsWazoApiConnection connection = getConnection(WazoEndpoints.CALLD)) {
			connection.executePost(payload, "X-Auth-Token", getToken());
			connection.manageApiResponse(201);
		}
	}

}
