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
package net.bluemind.ui.adminconsole.directory.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

public interface DirectoryCenterConstants extends Constants {

	public static final DirectoryCenterConstants INST = GWT.create(DirectoryCenterConstants.class);

	String selectColumn();

	String typeColumn();

	String loginColumn();

	String displayNameColumn();

	String newButton();

	String domainColumn();

	String delete();

	String all();

	String addFilter();

	String deleteConfirmation();

	String massDeleteConfirmation();

	String allMembers();

	String activeMembers();

	String suspendedMembers();

	String emailColumn();

	String orgUnitColumn();

	String fullAccount();

	String basicAccount();

	String visioAccount();

}
