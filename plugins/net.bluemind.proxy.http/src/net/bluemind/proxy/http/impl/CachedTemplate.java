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
package net.bluemind.proxy.http.impl;

import java.util.Date;

public final class CachedTemplate {

	private final byte[] content;
	private final Date lastModified;
	private final String contentType;

	public CachedTemplate(byte[] content, long lastModified, String contentType) {
		this.content = content;
		// remove 2sec to be safe with chrome sending equal time
		this.lastModified = new Date(lastModified - 2000);
		this.contentType = contentType;
	}

	public byte[] getContent() {
		return content;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public String getContentType() {
		return contentType;
	}

}
