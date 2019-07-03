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

import java.util.List;

import javax.xml.namespace.QName;

import net.bluemind.dav.server.proto.report.ReportResponse;
import net.bluemind.dav.server.store.ResType;

public class SyncCollectionResponse extends ReportResponse {

	private String newToken;
	private List<Create> creates;
	private List<Update> updates;
	private List<Remove> removals;
	private List<QName> props;
	private ResType rt;

	protected SyncCollectionResponse(String href, QName kind, ResType rt, String newToken, List<Create> creates,
			List<Update> updates, List<Remove> removals, List<QName> props) {
		super(href, kind);
		this.rt = rt;
		this.newToken = newToken;
		this.creates = creates;
		this.updates = updates;
		this.removals = removals;
		this.props = props;
	}

	public String getNewToken() {
		return newToken;
	}

	public List<Create> getCreates() {
		return creates;
	}

	public List<Update> getUpdates() {
		return updates;
	}

	public List<Remove> getRemovals() {
		return removals;
	}

	public List<QName> getProps() {
		return props;
	}

	public ResType getType() {
		return rt;
	}

}
