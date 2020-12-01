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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.common.io.FileBackedOutputStream;
import net.bluemind.node.client.impl.DoesNotExist;
import net.bluemind.node.client.impl.NodeRuntimeException;
import net.bluemind.node.shared.ActiveExecQuery;
import net.bluemind.node.shared.ExecDescriptor;

public class ActiveExecutionsHandler extends DefaultAsyncHandler<List<ExecDescriptor>> {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ActiveExecutionsHandler.class);
	private ActiveExecQuery query;

	public ActiveExecutionsHandler(ActiveExecQuery query) {
		super(true);
		this.query = query;
	}

	public BoundRequestBuilder prepare(BoundRequestBuilder rb) {
		if (query.group != null) {
			rb.addQueryParam("group", query.group);
			if (query.name != null) {
				rb.addQueryParam("name", query.name);
			}
		}
		return rb;
	}

	@Override
	protected List<ExecDescriptor> getResult(int status, HttpHeaders headers, FileBackedOutputStream body) {
		try {
			byte[] bytes = body.asByteSource().read();
			JsonObject jso = new JsonObject(new String(bytes));
			logger.info("Got {}", jso.encodePrettily());
			JsonArray descs = jso.getJsonArray("descriptors");
			int len = descs.size();
			List<ExecDescriptor> ret = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				JsonObject descJs = descs.getJsonObject(i);
				ExecDescriptor desc = new ExecDescriptor();
				desc.command = descJs.getString("command");
				desc.group = descJs.getString("group");
				desc.name = descJs.getString("name");
				desc.taskRefId = descJs.getString("pid");
				ret.add(desc);
			}
			return ret;
		} catch (IOException e) {
			throw NodeRuntimeException.wrap(e);
		} finally {
			try {
				body.reset();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public State onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
		int st = responseStatus.getStatusCode();
		if (st != 200) {
			RuntimeException t = null;
			if (st == 404) {
				t = new DoesNotExist();
			} else {
				t = new RuntimeException("list error: " + status);
			}
			throw t;
		} else {
			return super.onStatusReceived(responseStatus);
		}
	}
}
