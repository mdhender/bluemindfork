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
package net.bluemind.ui.adminconsole.directory.user.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface UserConstants extends Messages {

	public static final UserConstants INST = GWT.create(UserConstants.class);

	String editTitle(String name);

	String login();

	String generalTab();

	String accountName();

	String firstName();

	String lastName();

	String displayName();

	String password();

	String confirmPassword();

	String passwordLastChange();

	String passwordMustChange();

	String direction();

	String desc();

	String mail();

	String forward();

	String vacation();

	String mailTab();

	String contactTab();

	String enableMail();

	String delegation();

	String delegationTarget();

	String profile();

	String archive();

	String mailBackend();

	String quota();

	String changePicture();

	String passwordMismatch();

	String calPermsTab();

	String mboxPermsTab();

	String deviceTab();

	String mobileDevices();

	String identity();

	String type();

	String lastSync();

	String noDevice();

	String confirmDevicePartnershipRemoval();

	String confirmDevicePartnershipAddition();

	String confirmDeviceRemoveSyncKeys();

	String deviceRemoveSyncKeys();

	String customEmail();

	String outOfBMEmail();

	String extMailDesc();

	String routingInternal();

	String routingExternal();

	String routingNone();

	String addPartnership();

	String removePartnership();

	String partnership();

	String hideFromGal();

	String confirmDeviceRemoval(String id);

	String asUser();

	String groups();

	String editGroupMembership();

	String storagePolicyTab();

	String mailSettings();

	String calendarSettings();

	String contactsSettings();

	String clientSettings();

	String maintenanceTab();

	String mailboxReindex();

	String clearLocalData();

	String wipeDevice();

	String unwipeDevice();

	String confirmWipeDevice(String user, String device);

	String confirmWipeDevicePromptPlaceholder();

	String confirmWipeDeviceWarning();

	String confirmWipeDeviceWarningMsg();

	String confirmWipeDevicePromptCheckFail();

	String confirmUnwipeDevice(String user, String device);

	String refreshDevicesList();

	String newUser();

	String roles();

	String mailboxSharing();

	String validateUser();

	String checkAndRepair();

	String execute();

	String accountType();

	String accountTypeFull();

	String accountTypeSimple();

	String accountTypeSwitchToFull();

	String accountTypeSwitchToFullConfirm();

}
