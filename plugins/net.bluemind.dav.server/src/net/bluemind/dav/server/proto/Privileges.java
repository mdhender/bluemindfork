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
package net.bluemind.dav.server.proto;

import javax.xml.namespace.QName;

public final class Privileges {

	public static final QName ALL = QN.qn(NS.WEBDAV, "all");
	public static final QName READ = QN.qn(NS.WEBDAV, "read");
	public static final QName WRITE = QN.qn(NS.WEBDAV, "write");
	public static final QName WRITE_PROPS = QN.qn(NS.WEBDAV, "write-properties");
	public static final QName WRITE_CONTENT = QN.qn(NS.WEBDAV, "write-content");
	public static final QName BIND = QN.qn(NS.WEBDAV, "bind");
	public static final QName UNBIND = QN.qn(NS.WEBDAV, "unbind");
	public static final QName UNLOCK = QN.qn(NS.WEBDAV, "unlock");
	public static final QName READ_ACL = QN.qn(NS.WEBDAV, "read-acl");
	public static final QName WRITE_ACL = QN.qn(NS.WEBDAV, "write-acl");
	public static final QName READ_CU_PRIV_SET = QN.qn(NS.WEBDAV, "read-current-user-privilege-set");

	public static final QName READ_FREE_BUSY = QN.qn(NS.CALDAV, "read-free-busy");
	public static final QName SCHEDULE_DELIVER = QN.qn(NS.CALDAV, "schedule-deliver");
	public static final QName SCHEDULE_SEND = QN.qn(NS.CALDAV, "schedule-send");
	public static final QName SCHEDULE = QN.qn(NS.CALDAV, "schedule");

}
