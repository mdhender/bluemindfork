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
package net.bluemind.dav.server.proto.propfind;

import java.util.Set;

import javax.xml.namespace.QName;

import net.bluemind.dav.server.proto.DavQuery;
import net.bluemind.dav.server.store.DavResource;

public class PropFindQuery extends DavQuery {

	private final Set<QName> queried;
	private boolean allProps;

	public PropFindQuery(DavResource dr, Set<QName> queried) {
		super(dr);
		this.queried = queried;
	}

	public Set<QName> getQueried() {
		return queried;
	}

	public boolean isAllProps() {
		return allProps;
	}

	public void setAllProps(boolean allProps) {
		this.allProps = allProps;
	}

}
