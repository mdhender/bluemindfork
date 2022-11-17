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
package net.bluemind.role.service.internal;

import static net.bluemind.role.api.BasicRoles.CATEGORY_ADMINISTRATION;
import static net.bluemind.role.api.BasicRoles.CATEGORY_APPS;
import static net.bluemind.role.api.BasicRoles.CATEGORY_GENERAL;
import static net.bluemind.role.api.BasicRoles.CATEGORY_MAIL;
import static net.bluemind.role.api.BasicRoles.ROLE_ADMIN;
import static net.bluemind.role.api.BasicRoles.ROLE_API_DOCS;
import static net.bluemind.role.api.BasicRoles.ROLE_CALENDAR;
import static net.bluemind.role.api.BasicRoles.ROLE_CTI;
import static net.bluemind.role.api.BasicRoles.ROLE_DAV;
import static net.bluemind.role.api.BasicRoles.ROLE_EAS;
import static net.bluemind.role.api.BasicRoles.ROLE_EXTERNAL_IDENTITY;
import static net.bluemind.role.api.BasicRoles.ROLE_IM;
import static net.bluemind.role.api.BasicRoles.ROLE_MAIL;
import static net.bluemind.role.api.BasicRoles.ROLE_MAIL_FORWARDING;
import static net.bluemind.role.api.BasicRoles.ROLE_OUTLOOK;
import static net.bluemind.role.api.BasicRoles.ROLE_SELF_CHANGE_MAIL_IDENTITIES;
import static net.bluemind.role.api.BasicRoles.ROLE_SELF_CHANGE_PASSWORD;
import static net.bluemind.role.api.BasicRoles.ROLE_SELF_CHANGE_SETTINGS;
import static net.bluemind.role.api.BasicRoles.ROLE_TBIRD;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.role.api.RoleDescriptor;
import net.bluemind.role.api.RolesCategory;
import net.bluemind.role.provider.IRolesProvider;

public class BaseRolesProvider implements IRolesProvider {

	@Override
	public Set<String> getRoles() {
		return ImmutableSet.<String>builder().add(ROLE_IM, ROLE_CTI, ROLE_MAIL, ROLE_CALENDAR, ROLE_EAS, ROLE_DAV,
				ROLE_MAIL_FORWARDING, ROLE_EXTERNAL_IDENTITY, ROLE_TBIRD, ROLE_OUTLOOK).build();
	}

	@Override
	public Set<RoleDescriptor> getDescriptors(Locale locale) {
		ResourceBundle rb = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", locale);

		return ImmutableSet.<RoleDescriptor>builder().add( //

				RoleDescriptor
						.create(ROLE_IM, CATEGORY_APPS, rb.getString("role.im.label"),
								rb.getString("role.im.description"))//
						.priotity(998).delegable(), //

				RoleDescriptor
						.create(ROLE_CTI, CATEGORY_APPS, rb.getString("role.cti.label"),
								rb.getString("role.cti.description"))//
						.priotity(900).delegable(), //

				RoleDescriptor
						.create(ROLE_MAIL, CATEGORY_APPS, rb.getString("role.mail.label"),
								rb.getString("role.mail.description"))//
						.priotity(1000).delegable(), //

				RoleDescriptor
						.create(ROLE_CALENDAR, CATEGORY_APPS, rb.getString("role.calendar.label"),
								rb.getString("role.calendar.description"))//
						.priotity(999).delegable(), //

				RoleDescriptor
						.create(ROLE_EAS, CATEGORY_APPS, rb.getString("role.eas.label"),
								rb.getString("role.eas.description"))//
						.priotity(997).delegable(), //

				RoleDescriptor
						.create(ROLE_DAV, CATEGORY_APPS, rb.getString("role.dav.label"),
								rb.getString("role.dav.description"))//
						.priotity(996).delegable(), //

				RoleDescriptor
						.create(ROLE_TBIRD, CATEGORY_APPS, rb.getString("role.tbird.label"),
								rb.getString("role.tbird.description"))//
						.priotity(995).delegable(), //

				RoleDescriptor
						.create(ROLE_OUTLOOK, CATEGORY_APPS, rb.getString("role.outlook.label"),
								rb.getString("role.outlook.description"))//
						.priotity(994).delegable(), //

				RoleDescriptor
						.create(BasicRoles.ROLE_ADMINCONSOLE, CATEGORY_APPS, rb.getString("role.adminconsole.label"),
								rb.getString("role.adminconsole.description"))
						.withParent(SecurityContext.ROLE_SYSTEM).giveRoles(BasicRoles.ROLE_MANAGER) //
						.priotity(100).delegable(), //

				RoleDescriptor.create(ROLE_MAIL_FORWARDING, CATEGORY_MAIL, rb.getString("role.mailforwarding.label"),
						rb.getString("role.mailforwarding.description")).delegable(), //

				RoleDescriptor
						.create(ROLE_SELF_CHANGE_PASSWORD, CATEGORY_GENERAL,
								rb.getString("role.selfChangePassword.label"),
								rb.getString("role.selfChangePassword.description"))//
						.priotity(900), //

				RoleDescriptor.create(BasicRoles.ROLE_SELF_CHANGE_VCARD, CATEGORY_GENERAL,
						rb.getString("role.selfChangeVCard.label"), rb.getString("role.selfChangeVCard.description"))
						.priotity(800) //
						.withSelfPromote(BasicRoles.ROLE_MANAGE_USER_VCARD), //

				RoleDescriptor
						.create(BasicRoles.ROLE_SELF_MANAGE_EXTERNAL_ACCOUNT, CATEGORY_GENERAL,
								rb.getString("role.selfManageExternalAccount.label"),
								rb.getString("role.selfManageExternalAccount.description"))
						.withSelfPromote(BasicRoles.ROLE_MANAGE_EXTERNAL_ACCOUNTS), //

				RoleDescriptor
						.create(ROLE_SELF_CHANGE_SETTINGS, CATEGORY_GENERAL,
								rb.getString("role.selfChangeSettings.label"),
								rb.getString("role.selfChangeSettings.description"))
						.priotity(1000) //
						.withSelfPromote(BasicRoles.ROLE_MANAGE_USER_SETTINGS), //

				RoleDescriptor
						.create(ROLE_SELF_CHANGE_MAIL_IDENTITIES, CATEGORY_MAIL,
								rb.getString("role.selfChangeMailIdentities.label"),
								rb.getString("role.selfChangeMailIdentities.description"))
						.withSelfPromote(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES), //

				RoleDescriptor
						.create(BasicRoles.ROLE_SELF_CHANGE_MAILBOX_FILTER, CATEGORY_MAIL,
								rb.getString("role.selfChangeMailboxFilter.label"),
								rb.getString("role.selfChangeMailboxFilter.description"))
						.withSelfPromote(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER), //

				RoleDescriptor
						.create(ROLE_ADMIN, CATEGORY_ADMINISTRATION, rb.getString("role.admin.label"),
								rb.getString("role.admin.description"))
						.forDirEntry(Kind.DOMAIN).withParent(BasicRoles.ROLE_MANAGE_DOMAIN)
						.giveRoles(BasicRoles.ROLE_MANAGER), //

				RoleDescriptor.create(BasicRoles.ROLE_DOMAIN_MAX_VALUES, CATEGORY_ADMINISTRATION,
						rb.getString("role.domainMaxValues.label"), rb.getString("role.domainMaxValues.description"))
						.withParent(BasicRoles.ROLE_SYSTEM_MANAGER) //
						.giveRoles(BasicRoles.ROLE_MANAGER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_USER, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageUser.label"), rb.getString("role.manageUser.description"))
						.forDirEntry(Kind.USER) //
						.withParent(BasicRoles.ROLE_ADMIN) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_SUDO, CATEGORY_ADMINISTRATION, rb.getString("role.sudo.label"),
								rb.getString("role.sudo.description")) //
						.forDirEntry(Kind.USER) //
						.withParent(SecurityContext.ROLE_SYSTEM), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_USER_PASSWORD, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageUserPassword.label"),
								rb.getString("role.manageUserPassword.description"))
						.forDirEntry(Kind.USER) //
						.withParent(BasicRoles.ROLE_MANAGE_USER) //
						.giveRoles(BasicRoles.ROLE_MANAGER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_USER_VCARD, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageUserVCard.label"),
								rb.getString("role.manageUserVCard.description"))
						.forDirEntry(Kind.USER) //
						.withParent(BasicRoles.ROLE_MANAGE_USER) //
						.giveRoles(BasicRoles.ROLE_MANAGER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_EXTERNAL_ACCOUNTS, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageExternalAccounts.label"),
								rb.getString("role.manageExternalAccounts.description"))
						.forDirEntry(Kind.USER) //
						.withParent(BasicRoles.ROLE_MANAGE_USER),

				RoleDescriptor.create(BasicRoles.ROLE_MANAGER, CATEGORY_ADMINISTRATION, "fake", "fake").notVisible(),

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_USER_SETTINGS, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageUserSettings.label"),
								rb.getString("role.manageUserSettings.description"))
						.forDirEntry(Kind.USER) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_MANAGE_USER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_USER_DEVICE, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageUserDevice.label"),
								rb.getString("role.manageUserDevice.description"))
						.forDirEntry(Kind.USER) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_MANAGE_USER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_USER_SHARINGS, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageUserSharings.label"),
								rb.getString("role.manageUserSharings.description"))
						.withParent(BasicRoles.ROLE_MANAGE_USER) //
						.forDirEntry(Kind.USER) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_USER_SUBSCRIPTIONS, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageUserSubscriptions.label"),
								rb.getString("role.manageUserSubscriptions.description"))
						.forDirEntry(Kind.USER) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_MANAGE_USER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor.create(BasicRoles.ROLE_SELF_MANAGE_DEVICE, CATEGORY_GENERAL,
						rb.getString("role.selfManageDevice.label"), rb.getString("role.selfManageDevice.description"))
						.withSelfPromote(BasicRoles.ROLE_MANAGE_USER_DEVICE), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageUserMailIdentities.label"),
								rb.getString("role.manageUserMailIdentities.description"))
						.withParent(BasicRoles.ROLE_MANAGE_USER) //
						.forDirEntry(Kind.USER) //
						.giveRoles(BasicRoles.ROLE_MANAGE_MAILBOX, BasicRoles.ROLE_MANAGER),

				RoleDescriptor
						.create(ROLE_EXTERNAL_IDENTITY, CATEGORY_MAIL, rb.getString("role.externalidentity.label"),
								rb.getString("role.admin.description"))
						.withParent(BasicRoles.ROLE_MANAGE_USER) //
						.forDirEntry(Kind.USER) //
						.giveRoles(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES, BasicRoles.ROLE_MANAGER), //

				RoleDescriptor
						.create(ROLE_API_DOCS, CATEGORY_APPS, rb.getString("role.apiDocs.label"),
								rb.getString("role.apiDocs.description"))//
						.priotity(-1).delegable(), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_GROUP, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageGroup.label"), rb.getString("role.manageGroup.description"))
						.forDirEntry(Kind.GROUP) //
						.withParent(BasicRoles.ROLE_ADMIN) //
						.giveRoles(BasicRoles.ROLE_MANAGE_MAILBOX, BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_GROUP_MEMBERS, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageGroupMembers.label"),
								rb.getString("role.manageGroupMembers.description"))
						.withParent(BasicRoles.ROLE_MANAGE_GROUP) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.forDirEntry(Kind.GROUP), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_GROUP_SHARINGS, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageGroupSharings.label"),
								rb.getString("role.manageGroupSharings.description"))
						.withParent(BasicRoles.ROLE_MANAGE_GROUP).forDirEntry(Kind.GROUP) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_MAILBOX, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageMailbox.label"),
								rb.getString("role.manageMailbox.description"))
						.withParent(BasicRoles.ROLE_ADMIN) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.notVisible().withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_MAILBOX_FILTER, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageMailboxFilter.label"),
								rb.getString("role.manageMailboxFilter.description"))
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_MANAGE_MAILBOX).notVisible(), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageMailboxIdentities.label"),
								rb.getString("role.manageMailboxIdentities.description"))
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_MANAGE_MAILBOX).notVisible(), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_RESOURCE, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageResource.label"),
								rb.getString("role.manageResource.description"))
						.forDirEntry(Kind.RESOURCE) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_ADMIN) //
						.withContainerRoles(Verb.Manage.name()),

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_RESOURCE_SHARINGS, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageResourceSharings.label"),
								rb.getString("role.manageResourceSharings.description"))
						.forDirEntry(Kind.RESOURCE) //
						.withParent(BasicRoles.ROLE_MANAGE_RESOURCE) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_RESOURCE_TYPE, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageResourceType.label"),
								rb.getString("role.manageResourceType.description"))
						.forDirEntry(Kind.DOMAIN) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_ADMIN), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_MAILSHARE, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageMailshare.label"),
								rb.getString("role.manageMailshare.description"))
						.withParent(BasicRoles.ROLE_ADMIN)//
						.forDirEntry(Kind.MAILSHARE) //
						.giveRoles(BasicRoles.ROLE_MANAGE_MAILBOX, BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_MAILSHARE_SHARINGS, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageMailshareSharings.label"),
								rb.getString("role.manageMailshareSharings.description"))
						.forDirEntry(Kind.MAILSHARE) //
						.withParent(BasicRoles.ROLE_MANAGE_MAILSHARE)// *
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_DOMAIN_AB, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageDomainAB.label"),
								rb.getString("role.manageDomainAB.description"))
						.forDirEntry(Kind.ADDRESSBOOK) //
						.withParent(BasicRoles.ROLE_ADMIN) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_DOMAIN_LDAP_AB, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageDomainLDAPAB.label"),
								rb.getString("role.manageDomainLDAPAB.description"))
						.forDirEntry(Kind.ADDRESSBOOK) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_ADMIN), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_DOMAIN_AB_SHARING, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageDomainABSharings.label"),
								rb.getString("role.manageDomainABSharings.description"))
						.forDirEntry(Kind.ADDRESSBOOK)//
						.withParent(BasicRoles.ROLE_MANAGE_DOMAIN_AB)//
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_DOMAIN_CAL, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageDomainCal.label"),
								rb.getString("role.manageDomainCal.description"))
						.forDirEntry(Kind.CALENDAR) //
						.withParent(BasicRoles.ROLE_ADMIN) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_DOMAIN_CAL_SHARING, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageDomainCalSharings.label"),
								rb.getString("role.manageDomainCalSharings.description"))
						.forDirEntry(Kind.CALENDAR) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_MANAGE_DOMAIN_CAL) //
						.withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor.create(BasicRoles.ROLE_SYSTEM_MANAGER, CATEGORY_ADMINISTRATION,
						rb.getString("role.systemManager.label"), rb.getString("role.systemManager.description")), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_SERVER, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageServer.label"), rb.getString("role.manageServer.description"))
						.withParent(BasicRoles.ROLE_SYSTEM_MANAGER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_DOMAIN, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageDomain.label"), rb.getString("role.manageDomain.description"))
						.withParent(BasicRoles.ROLE_SYSTEM_MANAGER).withContainerRoles(Verb.Manage.name()), //

				RoleDescriptor.create(BasicRoles.ROLE_MANAGE_SYSTEM_CONF, CATEGORY_ADMINISTRATION,
						rb.getString("role.manageSystemConf.label"), rb.getString("role.manageSystemConf.description"))
						.withParent(BasicRoles.ROLE_SYSTEM_MANAGER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_SUBSCRIPTION, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageSubscription.label"),
								rb.getString("role.manageSubscription.description"))
						.withParent(BasicRoles.ROLE_SYSTEM_MANAGER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_OU, CATEGORY_ADMINISTRATION, rb.getString("role.manageOU.label"),
								rb.getString("role.manageOU.description"))
						.withParent(BasicRoles.ROLE_ADMIN)//
						.forDirEntry(Kind.ORG_UNIT) //
						.giveRoles(BasicRoles.ROLE_MANAGER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_SHOW_OU, CATEGORY_ADMINISTRATION, rb.getString("role.showOU.label"),
								rb.getString("role.showOU.description"))
						.withParent(BasicRoles.ROLE_MANAGE_OU)//
						.forDirEntry(Kind.ORG_UNIT) //
						.giveRoles(BasicRoles.ROLE_MANAGER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_DATAPROTECT, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageDataProtect.label"),
								rb.getString("role.manageDataProtect.description"))
						.withParent(BasicRoles.ROLE_SYSTEM_MANAGER),

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_RESTORE, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageRestore.label"),
								rb.getString("role.manageRestore.description"))
						.forDirEntry(Kind.DOMAIN) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_ADMIN), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_EXTERNAL_USER, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageExternalUser.label"),
								rb.getString("role.manageExternalUser.description"))
						.forDirEntry(Kind.EXTERNALUSER) //
						.withParent(BasicRoles.ROLE_ADMIN), //

				RoleDescriptor
						.create(BasicRoles.ROLE_READ_DOMAIN_FILTER, CATEGORY_MAIL,
								rb.getString("role.readDomainFilters.label"),
								rb.getString("role.readDomainFilters.description")) //
						.withParent(BasicRoles.ROLE_ADMIN),

				RoleDescriptor
						.create(BasicRoles.ROLE_WEBMAIL, CATEGORY_MAIL,
								rb.getString("role.accessRoundcubeWebmail.label"),
								rb.getString("role.accessRoundcubeWebmail.description"))
						.giveRoles(BasicRoles.ROLE_MAIL).delegable(),

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_USER_EXTERNAL_ID, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageUserExternalId.label"),
								rb.getString("role.manageUserExternalId.description"))
						.forDirEntry(Kind.USER) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_MANAGE_USER),

				RoleDescriptor
						.create(BasicRoles.ROLE_USER_CHECK_AND_REPAIR, CATEGORY_ADMINISTRATION,
								rb.getString("role.userCheckAndRepair.label"),
								rb.getString("role.userCheckAndRepair.description"))
						.forDirEntry(Kind.USER) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_MANAGE_USER),

				RoleDescriptor
						.create(BasicRoles.ROLE_USER_MAILBOX_MAINTENANCE, CATEGORY_ADMINISTRATION,
								rb.getString("role.userMailboxMaintenance.label"),
								rb.getString("role.userMailboxMaintenance.description"))
						.forDirEntry(Kind.USER) //
						.giveRoles(BasicRoles.ROLE_MANAGER) //
						.withParent(BasicRoles.ROLE_MANAGE_USER), //

				RoleDescriptor
						.create(BasicRoles.ROLE_MANAGE_CERTIFICATE, CATEGORY_ADMINISTRATION,
								rb.getString("role.manageCertificate.label"),
								rb.getString("role.manageCertificate.description")) //
						.forDirEntry(Kind.USER) //
						.withParent(SecurityContext.ROLE_SYSTEM)

		).build();
	}

	@Override
	public Set<RolesCategory> getCategories(Locale locale) {
		ResourceBundle rb = ResourceBundle.getBundle("OSGI-INF/l10n/bundle", locale);

		return ImmutableSet.<RolesCategory>builder()
				.add(RolesCategory.create(CATEGORY_GENERAL, rb.getString("category.general"), 500),
						RolesCategory.create(CATEGORY_ADMINISTRATION, rb.getString("category.administration"), 1000),
						RolesCategory.create(CATEGORY_APPS, rb.getString("category.apps"), 600),
						RolesCategory.create(CATEGORY_MAIL, rb.getString("category.mail"), 400))
				.build();
	}

}
