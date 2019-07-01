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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.google.common.base.Throwables;
import net.bluemind.common.io.FileBackedOutputStream;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;

import net.bluemind.node.api.FileDescription;
import net.bluemind.node.client.impl.DoesNotExist;

public class ListFilesHandler extends DefaultAsyncHandler<List<FileDescription>> {

	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger(ListFilesHandler.class);

	public ListFilesHandler() {
		super(true);
	}

	@Override
	protected List<FileDescription> getResult(int status, HttpResponseHeaders headers, FileBackedOutputStream body) {
		try {
			byte[] bytes = body.asByteSource().read();
			JsonObject jso = new JsonObject(new String(bytes));
			JsonArray descs = jso.getArray("descriptions");
			int len = descs.size();
			List<FileDescription> ret = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				JsonObject fdo = descs.get(i);
				FileDescription desc = new FileDescription(fdo.getString("path"));
				boolean isDir = fdo.getBoolean("dir");
				desc.setDirectory(isDir);
				if (!isDir) {
					desc.setSize(fdo.getLong("size"));
				}
				ret.add(desc);
			}
			return ret;
		} catch (IOException e) {
			throw Throwables.propagate(e);
		} finally {
			try {
				body.reset();
			} catch (IOException e) {
			}
		}
	}

	@Override
	public com.ning.http.client.AsyncHandler.STATE onStatusReceived(HttpResponseStatus responseStatus)
			throws Exception {
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
