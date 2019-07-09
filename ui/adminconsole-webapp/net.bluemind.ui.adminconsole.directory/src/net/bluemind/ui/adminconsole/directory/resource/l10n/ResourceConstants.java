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
package net.bluemind.ui.adminconsole.directory.resource.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface ResourceConstants extends Messages {

	public static final ResourceConstants INST = GWT.create(ResourceConstants.class);

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

	String capacity();

	String managers();

	String mail();

	String type();

	String none();

	String generalInfo();

	String customProperties();

	String customPropIntegerErr(String t);

	String newResource();

	String reservationMode();

	String reservationModeOwner();

	String reservationModeAutoAccept();

	String reservationModeAutoAcceptRefuse();
}
