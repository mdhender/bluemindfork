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
package net.bluemind.mailshare.hook;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.mailshare.api.Mailshare;

public interface IMailshareHook {

	/**
	 * Called after a {@link Mailshare} creation in database
	 * 
	 * @param context
	 * @param uid
	 * @param mailshare
	 * @param domainUid
	 * @throws ServerFault
	 */
	void onCreate(BmContext context, String uid, Mailshare mailshare, String domainUid) throws ServerFault;

	/**
	 * Called after a {@link Mailshare} update in database
	 * 
	 * @param context
	 * @param uid
	 * @param mailshare
	 * @param domainUid
	 * @throws ServerFault
	 */
	void onUpdate(BmContext context, String uid, Mailshare mailshare, String domainUid) throws ServerFault;

	/**
	 * Called after a {@link Mailshare} deletion in database
	 * 
	 * @param context
	 * @param uid
	 * @param domainUid
	 * @throws ServerFault
	 */
	void onDelete(BmContext context, String uid, String domainUid) throws ServerFault;
}
