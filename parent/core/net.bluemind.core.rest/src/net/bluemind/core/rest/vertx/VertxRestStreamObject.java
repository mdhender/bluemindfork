/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.core.rest.vertx;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

@SuppressWarnings("serial")
public class VertxRestStreamObject extends JsonObject {

	public final Buffer data;

	public final boolean end;

	public VertxRestStreamObject(Buffer data, boolean end) {
		this.data = data;
		this.end = end;
	}

	@Override
	public String encode() {
		throw new RuntimeException("should not be called");
	}

	@Override
	public JsonObject copy() {
		return this;
	}

}
