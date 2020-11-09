/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.webmodule.server;

import io.netty.buffer.ByteBuf;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class PreEncodedObject {
	private JsonObject js;
	private ByteBuf encoded;

	public PreEncodedObject(JsonObject js) {
		this.js = js;
		this.encoded = js.toBuffer().getByteBuf();
	}

	public JsonObject json() {
		return js;
	}

	public Buffer buffer() {
		return Buffer.buffer(encoded);
	}
}