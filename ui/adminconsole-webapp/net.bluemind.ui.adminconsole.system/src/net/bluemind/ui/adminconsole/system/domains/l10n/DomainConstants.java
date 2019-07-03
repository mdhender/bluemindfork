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
package net.bluemind.ui.adminconsole.system.domains.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface DomainConstants extends Messages {

	public static final DomainConstants INST = GWT.create(DomainConstants.class);

	String name();

	String nameColumn();

	String mailServices();

	String shouldCreateAdmin();

	String adminLogin();

	String adminPassword();

	String editTitle(String domainName);

	String generalTab();

	String addressBooksTab();

	String mailSystemTab();

	String indexingTab();

	String bmServicesTab();

	String filtersTab();

	String archiveTab();

	String tagsTab();

	String definition();

	String instantMessagingTab();

	String generalFieldset();

	String aliases();

	String language();

	String description();

	String isActive();

	String propertiesFieldset();

	String alias();

	String rmDomain();

	String newDomain();

	String search();

	String hsmTab();

	String imTab();

	String domain();

	String add();

	String imAuth();

	String imPublicAuth();

	String imAuthorizedEntities();

	String archname();

	String indexing();

	String coreServer();

	String mqServer();

	String ssoProxy();

	String xmppServer();

	String adminConsole();

	String calendarApplication();

	String webmailApplication();

	String contactApplication();

	String settingsApplication();

	String defaultAppRedirector();

	String reverseProxy();

	String database();

	String influxDb();

	String internalMailServer();

	String mailRelay();

	String mailboxStorageServer();

	String mailboxConnectionServer();

	String mailArchiveServer();

	String mailSystemInfo();

	String mailStorage();

	String mailRouting();

	String relayforSplittedDomains();

	String fileHostingServer();

	String forwardUnknownEmails();

	String mailService();

	String createAdmin();

	String login();

	String password();

	String inbox();

	String outbox();

	String sent();

	String trash();

	String drafts();

	String maxUsers();

	String maxUsersTooltip();

	String invalidMaxUsers();

	String maxBasicAccount();

	String invalidMaxBasicAccount();

	String mailflowRules();

	String andRule();

	String orRule();

	String notRule();

	String senderInOURule();

	String senderInGroupRule();

	String matchAlwaysRule();

	String xorRule();

	String signatureAction();
	
	String isDisclaimer();
	
	String usePlaceholder();
	
	String removePrevious();

	String addAssignment();

	String removeAssignment();

	String addRule();

	String addAction();

	String removeAction();

	String executionMode();

	String delete();

	String position();

	String group();

	String recipientIsExternalRule();

	String recipientIsInternalRule();

	String domainDeleted(String domainName);

	String deletingDomain(String domainName);

	String sendDateIsBeforeRule();

	String sendDateIsAfterRule();
}
