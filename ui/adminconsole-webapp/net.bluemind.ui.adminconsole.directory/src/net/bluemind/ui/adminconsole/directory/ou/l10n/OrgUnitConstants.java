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
package net.bluemind.ui.adminconsole.directory.ou.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface OrgUnitConstants extends Messages {

	public static final OrgUnitConstants INST = GWT.create(OrgUnitConstants.class);

	public String name();

	public String parent();

	public String qCreate();

	public String qUpdate();

	public String delete();

	public String addFilter();

	public String deleteConfirmation();

	public String massDeleteConfirmation();

	public String notDeletedConfirmation(String toNotDeleteUnits, String toDeleteUnits);

	public String notMassDeletedConfirmation(String toNotDeleteUnits, String toDeleteUnits);

	public String forbiddenDeletion();

	public String browse();

	public String resName();

	public String resEmail();

	public String resUnit();

	public String resType();

	public String ouResourceTab();

	public String ouRoleTab();

	public String emptyResourceTable();

	public String notFoundResourceTable(String ouPathName);

	public String massNotFoundResourceTable(String nbPaths);

	public String emptyRoleTable();

	public String emptyRoleAdminTable(String ouPathName);

	public String resourceOuSelection(String ouPathName);

	public String massResourceOuSelection(String nbPaths);

	public String roleOuSelection(String ouPathName);

	public String massRoleOuSelection();

	public String forbiddenMultiEdition();

	public String forbiddenRootEdition();

	public String invalidOuName();
}
