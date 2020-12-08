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
package net.bluemind.node.client.impl.ahc;

import org.asynchttpclient.BoundRequestBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.node.shared.ExecRequest;

public class SubmitHandler extends DefaultAsyncHandler<TaskRef> {

	private final ExecRequest execReq;

	public SubmitHandler(ExecRequest req) {
		super("R '" + req.command + "'", false);
		this.execReq = req;
	}

	@Override
	public BoundRequestBuilder prepare(BoundRequestBuilder rb) {
		rb.addHeader("Content-Type", "application/json");
		ByteBuf jso = JsonHelper.toJson(execReq);
		rb.setBody(jso.nioBuffer());
		return rb;
	}

	@Override
	protected TaskRef getResult(int status, HttpHeaders headers, FileBackedOutputStream body) {
		if (status == 201) {
			String pid = headers.get("Pid");
			return TaskRef.create(pid);
		} else {
			throw new RuntimeException("Submit error: " + status);
		}
	}

}
