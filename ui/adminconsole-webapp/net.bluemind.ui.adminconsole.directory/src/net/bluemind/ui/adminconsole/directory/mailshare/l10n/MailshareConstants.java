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
package net.bluemind.ui.adminconsole.directory.mailshare.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface MailshareConstants extends Messages {

	public static final MailshareConstants INST = GWT.create(MailshareConstants.class);

	String name();

	String generalTab();

	String permsTab();

	String otherTab();

	String editTitle(String name);

	String admins();

	String adminGroups();

	String delegation();

	String quota();

	String description();

	String mailBackend();

	String mail();

	String routing();

	String vacation();

	String newMailshare();

	String aclRead();

	String aclWrite();

	String aclAdmin();

	String routingInternal();

	String routingExternal();

	String routingNone();

}
