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
package net.bluemind.common.vertx.msgpack.tests;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import io.vertx.core.json.jackson.VertxModule;

public class VertxWithAfterburnerPerfTests extends AbstractCodecPerfTests {

	@Override
	protected ObjectMapper mapper() {
		ObjectMapper om = new ObjectMapper();
		om.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		om.registerModule(new VertxModule());
		om.registerModule(new AfterburnerModule().setUseValueClassLoader(false));
		return om;
	}

}
