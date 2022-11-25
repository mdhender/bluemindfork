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

public class MoveHandler extends DefaultAsyncHandler<Boolean> {
	private final String src;
	private final String dst;

	public MoveHandler(String src, String dst) {
		super(true);
		this.src = src;
		this.dst = dst;
	}

	public static class MoveJsonHelper {
		private MoveJsonHelper() {
		}

		public static String toJsonString(String src, String dst) {
			StringWriter sw = new StringWriter(128);
			try (JsonGenerator generator = new JsonFactory().createGenerator(sw)) {
				createNode(generator, src, dst);
				return sw.toString();
			} catch (IOException e) {
				throw new ServerFault(e);
			}
		}

		public static ByteBuf toJsonBuf(String src, String dst) {
			ByteBuf buf = Unpooled.buffer();
			try (OutputStream out = new ByteBufOutputStream(buf);
					JsonGenerator generator = new JsonFactory().createGenerator(out, JsonEncoding.UTF8)) {
				createNode(generator, src, dst);
				return buf;
			} catch (IOException e) {
				throw new ServerFault(e);
			}
		}

		private static void createNode(JsonGenerator generator, String src, String dst) throws IOException {
			generator.writeStartObject();
			generator.writeStringField("src", src);
			generator.writeStringField("dst", dst);
			generator.writeEndObject();
			generator.flush();
		}
	}

	@Override
	public BoundRequestBuilder prepare(BoundRequestBuilder rb) {
		rb.addHeader("Content-Type", "application/json");
		rb.setBody(MoveJsonHelper.toJsonBuf(src, dst).nioBuffer());
		return rb;
	}

	@Override
	protected Boolean getResult(int status, HttpHeaders headers, FileBackedOutputStream body) {
		return status == 200;
	}

}