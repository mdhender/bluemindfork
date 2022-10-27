/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.node.client.impl.ahc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.node.shared.ExecRequest;

public class JsonHelper {

	private JsonHelper() {
	}

	private static final JsonFactory jf = new JsonFactory();

	public static ByteBuf toJson(ExecRequest execReq) {
		return toJson(execReq, null);
	}

	public static ByteBuf toJson(ExecRequest execReq, Long wsRid) {
		ByteBuf buf = Unpooled.buffer();
		try (OutputStream out = new ByteBufOutputStream(buf);
				JsonGenerator generator = jf.createGenerator(out, JsonEncoding.UTF8)) {
			produceNodes(execReq, wsRid, generator);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
		return buf;
	}

	private static void produceNodes(ExecRequest execReq, Long wsRid, JsonGenerator generator) throws IOException {
		generator.writeStartObject();
		generator.writeStringField("command", execReq.command);
		if (execReq.group != null) {
			generator.writeStringField("group", execReq.group);
		}
		if (execReq.name != null) {
			generator.writeStringField("name", execReq.name);
		}
		generator.writeArrayFieldStart("options");
		for (ExecRequest.Options opt : execReq.options) {
			generator.writeString(opt.name());
		}
		generator.writeEndArray();
		if (wsRid != null) {
			generator.writeNumberField("ws-rid", wsRid.longValue());
		}

		generator.writeEndObject();
	}

	public static String toJsonString(ExecRequest execReq, Long wsRid) {
		StringWriter sw = new StringWriter(128);
		try (JsonGenerator generator = jf.createGenerator(sw)) {
			produceNodes(execReq, wsRid, generator);
		} catch (IOException e) {
			throw new ServerFault(e);
		}
		return sw.toString();
	}

}
