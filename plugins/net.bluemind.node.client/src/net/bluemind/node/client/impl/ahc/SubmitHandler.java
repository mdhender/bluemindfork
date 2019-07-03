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

import org.vertx.java.core.json.JsonObject;

import net.bluemind.common.io.FileBackedOutputStream;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.HttpResponseHeaders;

import net.bluemind.core.task.api.TaskRef;
import net.bluemind.node.shared.ExecRequest;

public class SubmitHandler extends DefaultAsyncHandler<TaskRef> {

	private final ExecRequest execReq;

	public SubmitHandler(ExecRequest req) {
		super(false);
		this.execReq = req;
	}

	@Override
	public BoundRequestBuilder prepare(BoundRequestBuilder rb) {
		rb.addHeader("Content-Type", "application/json; charset=utf-8");
		JsonObject jso = JsonHelper.toJson(execReq);
		rb.setBody(jso.encode());
		return rb;
	}

	@Override
	protected TaskRef getResult(int status, HttpResponseHeaders headers, FileBackedOutputStream body) {
		if (status == 201) {
			String pid = headers.getHeaders().getFirstValue("Pid");
			return TaskRef.create(pid);
		} else {
			throw new RuntimeException("Submit error: " + status);
		}
	}

}
