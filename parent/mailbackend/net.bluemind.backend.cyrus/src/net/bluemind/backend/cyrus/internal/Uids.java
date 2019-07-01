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

package net.bluemind.backend.cyrus.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public final class Uids {

	// now we have 4 problems
	private static final Pattern USER_MBOX = Pattern.compile("mailbox_user/(.+)@(.+)");
	private static final Pattern SHARED_MBOX = Pattern.compile("mailbox_(.+)@(.+)");

	/**
	 * @param mailboxContainer
	 *            the uid of the mailbox container
	 * @return
	 * @throws ServerFault
	 */
	public static String mailboxToCyrus(String mailboxContainer) throws ServerFault {
		Matcher userMatch = USER_MBOX.matcher(mailboxContainer);
		Matcher sharedMatch = SHARED_MBOX.matcher(mailboxContainer);

		if (userMatch.find()) {
			String uid = userMatch.group(1);
			String domain = userMatch.group(2);
			IUser api = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domain);
			User u = api.getComplete(uid).value;
			return "user/" + u.login + "@" + domain;
		} else if (sharedMatch.find()) {
			String uid = sharedMatch.group(1);
			String domain = sharedMatch.group(2);
			return uid + "@" + domain;
		} else {
			throw new RuntimeException("'" + mailboxContainer + "' is not a recognized mailboxContainer.");
		}
	}

	/**
	 * @param mailboxContainer
	 *            the uid of the mailbox container
	 * @return
	 * @throws ServerFault
	 */
	public static String mailboxToItemUid(String mailboxContainer) throws ServerFault {
		Matcher userMatch = USER_MBOX.matcher(mailboxContainer);
		Matcher sharedMatch = SHARED_MBOX.matcher(mailboxContainer);

		if (userMatch.find()) {
			return userMatch.group(1);
		} else if (sharedMatch.find()) {
			return sharedMatch.group(1);
		} else {
			throw new RuntimeException("'" + mailboxContainer + "' is not a recognized mailboxContainer.");
		}
	}

}
