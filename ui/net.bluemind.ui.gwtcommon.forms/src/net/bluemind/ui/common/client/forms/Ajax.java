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
package net.bluemind.ui.common.client.forms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import net.bluemind.authentication.api.AuthUser;
import net.bluemind.core.context.SecurityContext;

public class Ajax {

	static {
		String rolesString = getRoles();
		String[] roles = rolesString.split(",");
		TOKEN = new SecurityContext(getSid(), getUserId(), Collections.<String> emptyList(), Arrays.asList(roles),
				getDomain());
	}

	public static SecurityContext TOKEN;

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

	public static native String getDefaultEmail() /*-{
    return $wnd.bmcSessionInfos['defaultEmail'];
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

	public static native String getDisplayName() /*-{
    return $wnd.bmcSessionInfos['formatedName'];
	}-*/;
	
	public static native String getAccountType() /*-{
    return $wnd.bmcSessionInfos['accountType'];
	}-*/;


	public static void setAuthUser(AuthUser authUser) {
		TOKEN = new SecurityContext(getSid(), getUserId(), Collections.<String> emptyList(),
				new ArrayList<>(authUser.roles), authUser.rolesByOU, getDomain(), null, null);
	}

}
