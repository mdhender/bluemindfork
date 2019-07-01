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
package net.bluemind.gwtconsoleapp.base.editor;

import java.util.Arrays;
import java.util.Collections;

import net.bluemind.core.context.SecurityContext;

// FIXME remove
// TODO make a nice class to retrieve bmSessionInfos
public class Ajax {

	static {
		String rolesString = getRoles();
		String[] roles = rolesString.split(",");
		TOKEN = new SecurityContext(getSid(), getUserId(), Collections.<String> emptyList(), Arrays.asList(roles),
				getDomain());
	}

	public static final SecurityContext TOKEN;

	public static native String getLang()
	/*-{
		return $wnd.bmcSessionInfos['lang'];
	}-*/;

	private static native String getSid() /*-{
											return $wnd.bmcSessionInfos['sid'];
											}-*/;

	public static native String getLogin() /*-{
											return $wnd.bmcSessionInfos['login'];
											}-*/;

	private static native String getUserId() /*-{
												return $wnd.bmcSessionInfos['userId'];
												}-*/;

	public static native String version() /*-{
											return $wnd.bmcSessionInfos['version'];
											}-*/;

	public static native String getAuthService() /*-{
													return $wnd.bmcSessionInfos['authService'];
													}-*/;

	private static native String getDomain() /*-{
												return $wnd.bmcSessionInfos['domain'];
												}-*/;

	private static native String getRoles()
	/*-{
		return $wnd.bmcSessionInfos['roles']; 
	 }-*/;

}
