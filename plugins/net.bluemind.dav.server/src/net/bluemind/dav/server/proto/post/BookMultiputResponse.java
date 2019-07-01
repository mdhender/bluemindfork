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
package net.bluemind.dav.server.proto.post;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.bluemind.dav.server.proto.report.webdav.Create;
import net.bluemind.dav.server.proto.report.webdav.Remove;
import net.bluemind.dav.server.proto.report.webdav.Update;

public class BookMultiputResponse extends PostResponse {

	private List<Create> created;
	private List<Update> updated;
	private List<Remove> removed;
	private final String path;

	public BookMultiputResponse(String path) {
		this.path = path;
		created = ImmutableList.of();
		updated = ImmutableList.of();
		removed = ImmutableList.of();
	}

	public List<Create> getCreated() {
		return created;
	}

	public void setCreated(List<Create> created) {
		this.created = created;
	}

	public List<Update> getUpdated() {
		return updated;
	}

	public void setUpdated(List<Update> updated) {
		this.updated = updated;
	}

	public List<Remove> getRemoved() {
		return removed;
	}

	public void setRemoved(List<Remove> removed) {
		this.removed = removed;
	}

	public String getPath() {
		return path;
	}

}
