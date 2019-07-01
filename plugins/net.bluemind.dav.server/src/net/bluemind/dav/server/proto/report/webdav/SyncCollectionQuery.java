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

import net.bluemind.dav.server.proto.report.ReportQuery;
import net.bluemind.dav.server.store.DavResource;

public class SyncCollectionQuery extends ReportQuery {

	private String syncToken;
	private List<QName> props;

	protected SyncCollectionQuery(DavResource dr, QName kind) {
		super(dr, kind);
	}

	public String getSyncToken() {
		return syncToken;
	}

	public void setSyncToken(String syncToken) {
		this.syncToken = syncToken;
	}

	public List<QName> getProps() {
		return props;
	}

	public void setProps(List<QName> props) {
		this.props = props;
	}

}
