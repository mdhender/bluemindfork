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

import net.bluemind.dav.server.proto.props.webdav.ResourceType;

/**
 * Resource types for {@link ResourceType} property
 */
public class Types {

	public static final QName COL = QN.qn(NS.WEBDAV, "collection");

	public static final QName CAL = QN.qn(NS.CALDAV, "calendar");

	public static final QName SCHED_INBOX = QN.qn(NS.CALDAV, "schedule-inbox");

	public static final QName SCHED_OUTBOX = QN.qn(NS.CALDAV, "schedule-outbox");

	public static final QName NOTIF = QN.qn(NS.CSRV_ORG, "notification");

	public static final QName FREEBUSY = QN.qn(NS.CSRV_ORG, "free-busy-url");

	public static final QName DROPBOX = QN.qn(NS.CSRV_ORG, "dropbox-home");

	public static final QName AB = QN.qn(NS.CARDDAV, "addressbook");

}
