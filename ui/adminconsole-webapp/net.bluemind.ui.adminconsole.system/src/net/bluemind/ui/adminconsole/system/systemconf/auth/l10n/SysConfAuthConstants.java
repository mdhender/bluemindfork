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
package net.bluemind.ui.adminconsole.system.systemconf.auth.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface SysConfAuthConstants extends Messages {

	public static final SysConfAuthConstants INST = GWT.create(SysConfAuthConstants.class);

	String authTab();

	String authTypeChoice();

	String authType();

	String authInternal();

	String authCAS();

	String authKerberos();

	String authParameters();

	String casUrl();

	String casDomain();

	String krbAdDomain();

	String krbAdIp();

	String krbDomain();

	String krbKeyUpload();

	String restartHps();

	String restartHpsMsg();

	String restartingHps();

	String restartingHpsMsg();

	String needToRestartHps();

	String krbSubmit();

	String defaultDomain();

	String domainList();

}
