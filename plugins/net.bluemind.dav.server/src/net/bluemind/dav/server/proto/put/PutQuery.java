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
package net.bluemind.dav.server.proto.put;

import io.vertx.core.buffer.Buffer;
import net.bluemind.dav.server.proto.DavQuery;
import net.bluemind.dav.server.store.DavResource;

public abstract class PutQuery extends DavQuery {

	private String extId;
	private String collection;
	private boolean create;

	protected PutQuery(DavResource dr) {
		super(dr);
	}

	public String getExtId() {
		return extId;
	}

	public void setExtId(String extId) {
		this.extId = extId;
	}

	public String getCollection() {
		return collection;
	}

	public void setCollection(String collection) {
		this.collection = collection;
	}

	public boolean isCreate() {
		return create;
	}

	public void setCreate(boolean create) {
		this.create = create;
	}

	protected static PutQuery of(DavResource dr, String contentType, Buffer b) {
		if (contentType == null) {
			throw new RuntimeException("Content type is not set");
		}

		if (contentType.startsWith("text/calendar")) {
			return new CalendarPutQuery(dr, b.toString());
		} else if (contentType.startsWith("text/vcard")) {
			return new AddressbookPutQuery(dr, b.toString());
		}

		throw new RuntimeException("Content type not supported: " + contentType);
	}

}
