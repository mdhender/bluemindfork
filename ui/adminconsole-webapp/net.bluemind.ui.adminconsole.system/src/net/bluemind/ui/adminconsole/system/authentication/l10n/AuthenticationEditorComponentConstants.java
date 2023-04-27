/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.ui.adminconsole.system.authentication.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

public interface AuthenticationEditorComponentConstants extends Constants {
	public static final AuthenticationEditorComponentConstants INST = GWT
			.create(AuthenticationEditorComponentConstants.class);

	String authTypeChoice();

	String authType();

	String authInternal();

	String authCAS();

	String authKerberos();

	String authExternal();

	String authParameters();

	String casUrl();

	String casUrlInvalid();

	String krbAdDomain();

	String krbAdDomainInvalid();

	String krbAdIp();

	String krbAdIpInvalid();

	String krbAdKeytabUpload();

	String keytabContentInvalid();

	String krbKtpassPrincNameLabel();

	String krbKtpassPrincNameHelp();

	String externalConfUrl();

	String externalConfUrlInvalid();

	String externalClientId();

	String externalClientIdInvalid();

	String externalClientSecret();

	String externalClientSecretInvalid();
}
