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

import javax.xml.namespace.QName;

import net.bluemind.dav.server.proto.NS;
import net.bluemind.dav.server.proto.QN;

public final class WDReports {

	public static final QName ACL_PRINCIPAL_PROP_SET = QN.qn(NS.WEBDAV, "acl-principal-prop-set");

	public static final QName PRINCIPAL_MATCH = QN.qn(NS.WEBDAV, "principal-match");

	public static final QName PRINCIPAL_PROPERTY_SEARCH = QN.qn(NS.WEBDAV, "principal-property-search");

	public static final QName EXPAND_PROPERTY = QN.qn(NS.WEBDAV, "expand-property");

	public static final QName SYNC_COLLECTION = QN.qn(NS.WEBDAV, "sync-collection");

}
