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
package net.bluemind.ui.adminconsole.security;

import com.google.gwt.core.client.JsArray;

import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContribution;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElementContributorUnwrapper;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.ui.adminconsole.security.certificate.CertificateEditor;
import net.bluemind.ui.adminconsole.security.iptables.IpTablesEditor;
import net.bluemind.ui.adminconsole.security.iptables.IpTablesModelHandler;

public class SecurityScreensContributor implements ScreenElementContributorUnwrapper {

	@Override
	public JsArray<ScreenElementContribution> contribution() {

		ScreenElementContribution ipTablesEditor = ScreenElementContribution.create(null, null,
				ScreenRoot.create("iptablesRules", IpTablesEditor.TYPE));

		ScreenElementContribution ipTablesModelHandler = ScreenElementContribution.create("iptablesRules",
				"modelHandlers", ModelHandler.create(null, IpTablesModelHandler.TYPE));

		ScreenElementContribution certificateEditor = ScreenElementContribution.create(null, null,
				ScreenRoot.create("proxyCert", CertificateEditor.TYPE));

		JsArray<ScreenElementContribution> contribs = JsArray.createArray().cast();
		contribs.push(ipTablesEditor);
		contribs.push(ipTablesModelHandler);
		contribs.push(certificateEditor);
		return contribs;
	}

}
