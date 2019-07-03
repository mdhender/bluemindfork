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

/**
 * An autocomplete request / user search
 */
public class PrincipalPropertySearchQuery extends ReportQuery {

	private List<PropMatch> matches;
	private List<QName> expectedResults;

	protected PrincipalPropertySearchQuery(DavResource path, QName root) {
		super(path, root);
	}

	public List<PropMatch> getMatches() {
		return matches;
	}

	public void setMatches(List<PropMatch> matches) {
		this.matches = matches;
	}

	public List<QName> getExpectedResults() {
		return expectedResults;
	}

	public void setExpectedResults(List<QName> expectedResults) {
		this.expectedResults = expectedResults;
	}

}
