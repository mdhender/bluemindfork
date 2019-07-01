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
package net.bluemind.dav.server.proto;

import net.bluemind.dav.server.store.DavResource;

public class DavQuery {

	private boolean brief;
	private Depth depth;
	private final DavResource resource;
	private String prefer;

	protected DavQuery(DavResource resource) {
		this.resource = resource;
	}

	public boolean isBrief() {
		return brief;
	}

	public Depth getDepth() {
		return depth;
	}

	public void setBrief(boolean brief) {
		this.brief = brief;
	}

	public void setDepth(Depth depth) {
		this.depth = depth;
	}

	public String getPath() {
		return resource.getPath();
	}

	public String getPrefer() {
		return prefer;
	}

	public void setPrefer(String prefer) {
		this.prefer = prefer;
	}

	public DavResource getResource() {
		return resource;
	}

}
