/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.node.client.impl.ahc;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import org.asynchttpclient.BoundRequestBuilder;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaders;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.api.fault.ServerFault;

public class MkdirsHandler extends DefaultAsyncHandler<Boolean> {
	private final String dst;
	private final String permissions;
	private final String owner;
	private final String group;

	public MkdirsHandler(String dst, String permissions, String owner, String group) {
		super(true);
		this.dst = dst;
		this.permissions = permissions;
		this.owner = owner;
		this.group = group;
	}

	public static class MkdirsJsonHelper {
		private MkdirsJsonHelper() {
		}

		public static String toJsonString(String dst, String permissions, String owner, String group) {
			StringWriter sw = new StringWriter(128);
			try (JsonGenerator generator = new JsonFactory().createGenerator(sw)) {
				createNode(generator, dst, permissions, owner, group);
				return sw.toString();
			} catch (IOException e) {
				throw new ServerFault(e);
			}
		}

		public static ByteBuf toJsonBuf(String dst, String permissions, String owner, String group) {
			ByteBuf buf = Unpooled.buffer();
			try (OutputStream out = new ByteBufOutputStream(buf);
					JsonGenerator generator = new JsonFactory().createGenerator(out, JsonEncoding.UTF8)) {
				createNode(generator, dst, permissions, owner, group);
				return buf;
			} catch (IOException e) {
				throw new ServerFault(e);
			}
		}

		private static void createNode(JsonGenerator generator, String dst, String permissions, String owner,
				String group) throws IOException {
			generator.writeStartObject();
			generator.writeStringField("dst", dst);
			generator.writeStringField("permissions", permissions);
			generator.writeStringField("owner", owner);
			generator.writeStringField("group", group);
			generator.writeEndObject();
			generator.flush();
		}
	}

	@Override
	public BoundRequestBuilder prepare(BoundRequestBuilder rb) {
		rb.addHeader("Content-Type", "application/json");
		rb.setBody(MkdirsJsonHelper.toJsonBuf(dst, permissions, owner, group).nioBuffer());
		return rb;
	}

	@Override
	protected Boolean getResult(int status, HttpHeaders headers, FileBackedOutputStream body) {
		return status == 200;
	}
}
