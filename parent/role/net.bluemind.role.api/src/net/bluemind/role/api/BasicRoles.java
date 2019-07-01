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
package net.bluemind.role.api;

public class BasicRoles {
	// Apps
	public static final String ROLE_CTI = "hasCTI";
	public static final String ROLE_IM = "hasIM";
	public static final String ROLE_MAIL = "hasMail";
	public static final String ROLE_CALENDAR = "hasCalendar";
	public static final String ROLE_ADMINCONSOLE = "hasAdminConsole";
	public static final String ROLE_EAS = "hasEAS";
	public static final String ROLE_DAV = "hasDAV";
	public static final String ROLE_TBIRD = "hasTbird";
	public static final String ROLE_OUTLOOK = "hasOutlook";

	public static final String ROLE_SUDO = "sudo";
	public static final String ROLE_MAIL_FORWARDING = "mailForwarding";
	public static final String ROLE_SELF_CHANGE_PASSWORD = "selfChangePassword";// NOSONAR
	public static final String ROLE_SELF_CHANGE_SETTINGS = "selfChangeSettings";
	public static final String ROLE_SELF_CHANGE_MAIL_IDENTITIES = "selfChangeMailIdentities";
	public static final String ROLE_SELF_CHANGE_MAILBOX_FILTER = "selfChangeMailboxFilter";
	public static final String ROLE_SELF_MANAGE_DEVICE = "selfManageDevice";
	public static final String ROLE_SELF_CHANGE_VCARD = "selfManageVCard";
	public static final String ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT = "selfManageExternalAccount";
	// FIXME rename domain admin
	public static final String ROLE_ADMIN = "admin";
	public static final String ROLE_MANAGER = "domainManager";
	public static final String ROLE_MANAGE_USER = "manageUser";
	public static final String ROLE_MANAGE_EXTERNAL_ACCOUNTS = "manageExternalAccounts";
	public static final String ROLE_MANAGE_USER_VCARD = "manageUserVCard";
	public static final String ROLE_MANAGE_USER_PASSWORD = "manageUserPassword";// NOSONAR
	public static final String ROLE_MANAGE_USER_SETTINGS = "managerUserSettings";
	public static final String ROLE_MANAGE_USER_MAIL_IDENTITIES = "managerUserMailIdentities";
	public static final String ROLE_MANAGE_USER_DEVICE = "manageUserDevice";
	public static final String ROLE_MANAGE_USER_SHARINGS = "manageUserSharings";
	public static final String ROLE_MANAGE_USER_SUBSCRIPTIONS = "manageUserSubscriptions";
	public static final String ROLE_MANAGE_GROUP = "manageGroup";
	public static final String ROLE_MANAGE_GROUP_SHARINGS = "manageGroupSharings";
	public static final String ROLE_MANAGE_GROUP_MEMBERS = "manageGroupMembers";
	public static final String ROLE_EXTERNAL_IDENTITY = "canCreateExternalIdentity";
	public static final String ROLE_API_DOCS = "apiDocs";

	public static final String CATEGORY_GENERAL = "general";
	public static final String CATEGORY_ADMINISTRATION = "administration";
	public static final String CATEGORY_MAIL = "mail";
	public static final String CATEGORY_APPS = "apps";
	public static final String SELF_CHANGE_PASSWORD = "selfChangePassword";// NOSONAR
	public static final String ROLE_SELF = "self";

	public static final String ROLE_MANAGE_ENTITY_SHARINGS = "manageSharings";
	public static final String ROLE_MANAGE_MAILBOX = "manageMailbox";
	public static final String ROLE_MANAGE_MAILBOX_FILTER = "manageMailboxFilter";
	public static final String ROLE_MANAGE_MAILBOX_IDENTITIES = "manageMailboxIdentities";
	public static final String ROLE_MANAGE_RESOURCE = "manageResource";
	public static final String ROLE_MANAGE_RESOURCE_TYPE = "manageResourceType";
	public static final String ROLE_MANAGE_RESOURCE_SHARINGS = "manageResourceSharings";
	public static final String ROLE_MANAGE_MAILSHARE = "manageMailshare";
	public static final String ROLE_MANAGE_MAILSHARE_SHARINGS = "manageMailshareSharings";
	public static final String ROLE_MANAGE_EXTERNAL_USER = "manageExternalUser";

	public static final String ROLE_MANAGE_DOMAIN_AB = "manageDomainAB";
	public static final String ROLE_MANAGE_DOMAIN_AB_SHARING = "manageDomainABSharings";
	public static final String ROLE_MANAGE_DOMAIN_LDAP_AB = "manageDomainLDAPAB";

	public static final String ROLE_MANAGE_DOMAIN_CAL = "manageDomainCal";
	public static final String ROLE_MANAGE_DOMAIN_CAL_SHARING = "manageDomainCalSharings";

	public static final String ROLE_MANAGE_SERVER = "manageServer";
	public static final String ROLE_MANAGE_DOMAIN = "manageDomain";
	public static final String ROLE_MANAGE_SYSTEM_CONF = "manageSystemConf";
	public static final String ROLE_SYSTEM_MANAGER = "systemManagement";
	public static final String ROLE_READ_DOMAIN_FILTER = "readDomainFilters";

	public static final String ROLE_DOMAIN_MAX_VALUES = "domainMaxValues";
	public static final String ROLE_MANAGE_SUBSCRIPTION = "manageSubscription";
	public static final String ROLE_MANAGE_OU = "manageOU";
	public static final String ROLE_DATAPROTECT = "manageDataProtect";
	public static final String ROLE_MANAGE_RESTORE = "manageRestore";
}
