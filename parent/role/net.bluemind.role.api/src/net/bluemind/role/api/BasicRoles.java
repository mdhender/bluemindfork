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

/**
 * Declaration of all standard roles.
 */
public class BasicRoles {
	/**
	 * Telephony.
	 */
	public static final String ROLE_CTI = "hasCTI";
	/**
	 * Instant Messaging.
	 */
	public static final String ROLE_IM = "hasIM";
	/**
	 * Webmail.
	 */
	public static final String ROLE_MAIL = "hasMail";
	/**
	 * Calendar App.
	 */
	public static final String ROLE_CALENDAR = "hasCalendar";
	/**
	 * Access to the Bluemind Administration.
	 */
	public static final String ROLE_ADMINCONSOLE = "hasAdminConsole";
	/**
	 * Synchronization of mobile devices.
	 */
	public static final String ROLE_EAS = "hasEAS";
	/**
	 * Synchronization with clients using the DAV protocol.
	 */
	public static final String ROLE_DAV = "hasDAV";
	/**
	 * Bluemind Thunderbird plugin.
	 */
	public static final String ROLE_TBIRD = "hasTbird";
	/**
	 * Bluemind Outlook plugin.
	 */
	public static final String ROLE_OUTLOOK = "hasOutlook";
	/**
	 * Permission to "login-as" other user.
	 */
	public static final String ROLE_SUDO = "sudo";
	/**
	 * Permission to forward mails to an external account.
	 */
	public static final String ROLE_MAIL_FORWARDING = "mailForwarding";
	/**
	 * Permission to change own password.
	 */
	public static final String ROLE_SELF_CHANGE_PASSWORD = "selfChangePassword";
	/**
	 * Permission to change own settings.
	 */
	public static final String ROLE_SELF_CHANGE_SETTINGS = "selfChangeSettings";
	/**
	 * Permission to change mail identities.
	 */
	public static final String ROLE_SELF_CHANGE_MAIL_IDENTITIES = "selfChangeMailIdentities";
	/**
	 * Permission to change mailbox filter.
	 */
	public static final String ROLE_SELF_CHANGE_MAILBOX_FILTER = "selfChangeMailboxFilter";
	/**
	 * Permission to manage own mobile devices.
	 */
	public static final String ROLE_SELF_MANAGE_DEVICE = "selfManageDevice";
	/**
	 * Permission to change own contact infos.
	 */
	public static final String ROLE_SELF_CHANGE_VCARD = "selfManageVCard";
	/**
	 * Permission to change own external accounts.
	 */
	public static final String ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT = "selfManageExternalAccount";
	/**
	 * System admin role.
	 */
	public static final String ROLE_ADMIN = "admin";
	/**
	 * Domain admin role.
	 */
	public static final String ROLE_MANAGER = "domainManager";
	/**
	 * Permission to manage domain users.
	 */
	public static final String ROLE_MANAGE_USER = "manageUser";
	/**
	 * Permission to manage external system accounts.
	 */
	public static final String ROLE_MANAGE_EXTERNAL_ACCOUNTS = "manageExternalAccounts";
	/**
	 * Permission to manage contact infos.
	 */
	public static final String ROLE_MANAGE_USER_VCARD = "manageUserVCard";
	/**
	 * Permission to change user's passwords.
	 */
	public static final String ROLE_MANAGE_USER_PASSWORD = "manageUserPassword";// NOSONAR
	/**
	 * Permission to change user's settings.
	 */
	public static final String ROLE_MANAGE_USER_SETTINGS = "managerUserSettings";
	/**
	 * Permission to change user's mail identities.
	 */
	public static final String ROLE_MANAGE_USER_MAIL_IDENTITIES = "managerUserMailIdentities";
	/**
	 * Permission to change user's mobile devices.
	 */
	public static final String ROLE_MANAGE_USER_DEVICE = "manageUserDevice";
	/**
	 * Permission to change user's sharings.
	 */
	public static final String ROLE_MANAGE_USER_SHARINGS = "manageUserSharings";
	/**
	 * Permission to change user's subscriptions.
	 */
	public static final String ROLE_MANAGE_USER_SUBSCRIPTIONS = "manageUserSubscriptions";
	/**
	 * Permission to manage groups.
	 */
	public static final String ROLE_MANAGE_GROUP = "manageGroup";
	/**
	 * Permission to manage group's sharings.
	 */
	public static final String ROLE_MANAGE_GROUP_SHARINGS = "manageGroupSharings";
	/**
	 * Permission to manage group members.
	 */
	public static final String ROLE_MANAGE_GROUP_MEMBERS = "manageGroupMembers";
	/**
	 * Permission to create external identity.
	 */
	public static final String ROLE_EXTERNAL_IDENTITY = "canCreateExternalIdentity";
	/**
	 * Permission to access interactive api documentation.
	 */
	public static final String ROLE_API_DOCS = "apiDocs";
	/**
	 * General configuration category.
	 */
	public static final String CATEGORY_GENERAL = "general";
	/**
	 * Administration category.
	 */
	public static final String CATEGORY_ADMINISTRATION = "administration";
	/**
	 * Messaging category.
	 */
	public static final String CATEGORY_MAIL = "mail";
	/**
	 * Bluemind applications category.
	 */
	public static final String CATEGORY_APPS = "apps";
	/**
	 * Permission to access own entities.
	 */
	public static final String ROLE_SELF = "self";
	/**
	 * Permission to manage sharings.
	 */
	public static final String ROLE_MANAGE_ENTITY_SHARINGS = "manageSharings";
	/**
	 * Permission to access/manage mailbox.
	 */
	public static final String ROLE_MANAGE_MAILBOX = "manageMailbox";
	/**
	 * Permission to manage mailbox filters.
	 */
	public static final String ROLE_MANAGE_MAILBOX_FILTER = "manageMailboxFilter";
	/**
	 * Permission to manage mail identities.
	 */
	public static final String ROLE_MANAGE_MAILBOX_IDENTITIES = "manageMailboxIdentities";
	/**
	 * Permission to manage resources.
	 */
	public static final String ROLE_MANAGE_RESOURCE = "manageResource";
	/**
	 * Permission to manage resource types.
	 */
	public static final String ROLE_MANAGE_RESOURCE_TYPE = "manageResourceType";
	/**
	 * Permission to manage resource sharings.
	 */
	public static final String ROLE_MANAGE_RESOURCE_SHARINGS = "manageResourceSharings";
	/**
	 * Permission to manage mailshares.
	 */
	public static final String ROLE_MANAGE_MAILSHARE = "manageMailshare";
	/**
	 * Permission to manage mailshare sharings.
	 */
	public static final String ROLE_MANAGE_MAILSHARE_SHARINGS = "manageMailshareSharings";
	/**
	 * Permission to manage external users.
	 */
	public static final String ROLE_MANAGE_EXTERNAL_USER = "manageExternalUser";
	/**
	 * Permission to manage the domain addressbook.
	 */
	public static final String ROLE_MANAGE_DOMAIN_AB = "manageDomainAB";
	/**
	 * Permission to manage the domain addressbook sharings.
	 */
	public static final String ROLE_MANAGE_DOMAIN_AB_SHARING = "manageDomainABSharings";
	/**
	 * Permission to manage the domain LDAP addressbook.
	 */
	public static final String ROLE_MANAGE_DOMAIN_LDAP_AB = "manageDomainLDAPAB";
	/**
	 * Permission to manage the domain calendar.
	 */
	public static final String ROLE_MANAGE_DOMAIN_CAL = "manageDomainCal";
	/**
	 * Permission to manage the domain calendar sharings.
	 */
	public static final String ROLE_MANAGE_DOMAIN_CAL_SHARING = "manageDomainCalSharings";
	/**
	 * Permission to manage the application servers.
	 */
	public static final String ROLE_MANAGE_SERVER = "manageServer";
	/**
	 * Permission to manage domains.
	 */
	public static final String ROLE_MANAGE_DOMAIN = "manageDomain";
	/**
	 * Permission to manage global system configuration.
	 */
	public static final String ROLE_MANAGE_SYSTEM_CONF = "manageSystemConf";
	/**
	 * Permission to manage the system (backup, sharding, etc.).
	 */
	public static final String ROLE_SYSTEM_MANAGER = "systemManagement";
	/**
	 * Permission to access domain mail-filters.
	 */
	public static final String ROLE_READ_DOMAIN_FILTER = "readDomainFilters";
	/**
	 * Permission to manage max user accounts/domain.
	 */
	public static final String ROLE_DOMAIN_MAX_VALUES = "domainMaxValues";
	/**
	 * Permission to manage the bluemind subscription.
	 */
	public static final String ROLE_MANAGE_SUBSCRIPTION = "manageSubscription";
	/**
	 * Permission to manage the organizational units.
	 */
	public static final String ROLE_MANAGE_OU = "manageOU";
	/**
	 * Permission to manage backups.
	 */
	public static final String ROLE_DATAPROTECT = "manageDataProtect";
	/**
	 * Permission to manage restores.
	 */
	public static final String ROLE_MANAGE_RESTORE = "manageRestore";
	/**
	 * Permission to access Roundcube webmail.
	 */
	public static final String ROLE_WEBMAIL = "hasWebmail";

	/**
	 * Permission to change user ext id.
	 */
	public static final String ROLE_MANAGE_USER_EXTERNAL_ID = "manageUserExternalId";

	/**
	 * Permission to c&r user.
	 */
	public static final String ROLE_USER_CHECK_AND_REPAIR = "userCheckAndRepair";

	/**
	 * Permission to reindex user's mbox.
	 */
	public static final String ROLE_USER_MAILBOX_MAINTENANCE = "userMailboxMaintenance";

}
