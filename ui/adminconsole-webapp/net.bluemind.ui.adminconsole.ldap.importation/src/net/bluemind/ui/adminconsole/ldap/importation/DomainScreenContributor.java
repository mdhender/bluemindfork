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
package net.bluemind.ui.adminconsole.ldap.importation;

import com.google.gwt.core.client.JsArray;

import net.bluemind.core.context.SecurityContext;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.Tab;
import net.bluemind.ui.adminconsole.ldap.importation.l10n.Ldap;

public class DomainScreenContributor implements ScreenElementContributorUnwrapper {

	@Override
	public JsArray<ScreenElementContribution> contribution() {
		ScreenElementContribution ldapTab = ScreenElementContribution.create("editDomainTabs", "tabs",
				Tab.create("ldapTab", Ldap.INST.tabName(),
						ScreenElement.create(null, EditDomainLdapEditor.TYPE).withRole(SecurityContext.ROLE_ADMIN)));

		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();
		contribs.push(ldapTab);
		return contribs;
	}
}
