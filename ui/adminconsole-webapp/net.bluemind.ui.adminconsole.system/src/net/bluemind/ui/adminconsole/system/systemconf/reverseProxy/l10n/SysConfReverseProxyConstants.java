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
package net.bluemind.ui.adminconsole.system.systemconf.reverseProxy.l10n;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface SysConfReverseProxyConstants extends Messages {

	public static final SysConfReverseProxyConstants INST = GWT.create(SysConfReverseProxyConstants.class);

	String nginx();

	String swPassword();

	String reverseProxyTab();

	String nginxWorker();

	String httpProxy();

	String httpProxyHelp();

	String httpProxyEnabled();

	String httpProxyHostname();

	String httpProxyPort();

	String httpProxyLogin();

	String httpProxyPassword();

	String httpProxyExceptions();

	String httpProxyExceptionsHelp();

	String externalUrl();

	String externalUrlHelp();

	String otherUrls();

	String otherUrlsHelp();

	String general();
}
