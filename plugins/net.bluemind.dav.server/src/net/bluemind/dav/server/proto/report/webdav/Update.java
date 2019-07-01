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
package net.bluemind.dav.server.proto.report.webdav;

public final class Update implements IChange {

	private String uuid;
	private long lastMod;
	private final String urlId;

	public Update(String uuid, long lastMod) {
		this(uuid, uuid, lastMod);
	}

	public Update(String urlId, String uuid, long lastMod) {
		this.urlId = urlId;
		this.uuid = uuid;
		this.lastMod = lastMod;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public long getLastMod() {
		return lastMod;
	}

	public void setLastMod(long lastMod) {
		this.lastMod = lastMod;
	}

	public String getUrlId() {
		return urlId;
	}

}
