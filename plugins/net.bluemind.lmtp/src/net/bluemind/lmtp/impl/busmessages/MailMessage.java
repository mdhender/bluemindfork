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
package net.bluemind.lmtp.impl.busmessages;

import org.vertx.java.core.json.JsonObject;

import io.netty.buffer.ByteBuf;
import net.bluemind.lmtp.backend.LmtpEnvelope;

@SuppressWarnings("serial")
public class MailMessage extends JsonObject {

	private LmtpEnvelope envelope;
	private ByteBuf data;

	public MailMessage(LmtpEnvelope mEnvelope, ByteBuf buffer) {
		envelope = mEnvelope;
		data = buffer;
	}

	@Override
	public String encode() {
		throw new RuntimeException("Not encodable");
	}

	@Override
	public JsonObject copy() {
		return this;
	}

	public LmtpEnvelope getEnvelope() {
		return envelope;
	}

	public ByteBuf getData() {
		return data;
	}

}
