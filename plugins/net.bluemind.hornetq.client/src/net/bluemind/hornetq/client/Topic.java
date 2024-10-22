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
package net.bluemind.hornetq.client;

/**
 * MQ topic names
 * 
 */
public final class Topic {

	private Topic() {
	}

	public static final String CACHE_FLUSH = "bm.cache.flush";
	public static final String CONTACT_NOTIFICATIONS = "bm.contact.notifications";
	public static final String CALENDAR_NOTIFICATIONS = "bm.calendar.notifications";
	public static final String DIRECTORY_NOTIFICATIONS = "bm.directory.notifications";
	public static final String MAILBOX_NOTIFICATIONS = "bm.mailbox.notifications";
	public static final String DATA_SERIALIZATION_NOTIFICATIONS = "bm.core.data.serialization.notifications";

	public static final String TASK_NOTIFICATIONS = "bm.task.notifications";
	public static final String CORE_NOTIFICATIONS = "bm.core.notifications";
	public static final String CORE_SESSIONS = "bm.core.session";
	public static final String SYSTEM_NOTIFICATIONS = "bm.system.notifications";
	public static final String XIVO_PHONE_STATUS = "xivo.phone.status";
	public static final String PRESENCE_NOTIFICATIONS = "bm.presence.notifications";
	public static final String IM_NOTIFICATIONS = "bm.im.notifications";
	public static final String HOOKS_DEVICE = "bm.core.hooks.device";
	public static final String MAILFLOW_NOTIFICATIONS = "bm.core.mailflow.notifications";

	public static final String IMAP_ITEM_NOTIFICATIONS = "imap.item.notifications";
	public static final String MAPI_ITEM_NOTIFICATIONS = "mapi.item.notifications";
	public static final String MAPI_DEFERRED_ACTION_NOTIFICATIONS = "mapi.deferred.action.notifications";
	public static final String MAPI_HIERARCHY_NOTIFICATIONS = "mapi.hierarchy.notifications";
	public static final String MAPI_DELEGATION_NOTIFICATIONS = "mapi.delegation.notifications";
	public static final String MAPI_PF_ACL_UPDATE = "mapi.pf.acls.changed";
	public static final String MAPI_REPAIRS = "mapi.repairs";

	public static final String UI_RESOURCES_NOTIFICATIONS = "bm.ui.resources.notifications";

	public static final String GLOBAL_SETTINGS_NOTIFICATIONS = "bm.global.settings.notifications";
	public static final String LOGBACK_CONFIG = "logback.configuration";
	public static final String SENTRY_CONFIG = "sentry.configuration";

	public static final String PRODUCT_CHECK_RESULTS = "product.check.result";
	public static final String PRODUCT_CHECK_REQUESTS = "product.check.req";

}
