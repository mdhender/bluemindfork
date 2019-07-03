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
package net.bluemind.dav.server.proto.report;

import javax.xml.namespace.QName;

import net.bluemind.dav.server.proto.DavQuery;
import net.bluemind.dav.server.store.DavResource;

public abstract class ReportQuery extends DavQuery {

	private final QName kind;

	protected ReportQuery(DavResource dr, QName kind) {
		super(dr);
		this.kind = kind;
	}

	public QName getKind() {
		return kind;
	}

}
