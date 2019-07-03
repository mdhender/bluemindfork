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
package net.bluemind.dav.server.proto.proppatch;

import java.util.Map;

import javax.xml.namespace.QName;

import net.bluemind.dav.server.store.DavResource;
import net.bluemind.dav.server.store.PropSetResult;

public class PropPatchResponse {

	private final DavResource resource;
	private Map<QName, PropSetResult> results;

	public PropPatchResponse(DavResource resource) {
		this.resource = resource;
	}

	public DavResource getResource() {
		return resource;
	}

	public void setResults(Map<QName, PropSetResult> results) {
		this.results = results;
	}

	public Map<QName, PropSetResult> getResults() {
		return results;
	}

}
