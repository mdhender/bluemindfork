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
package net.bluemind.authentication.api.incore;

import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;

public interface IInCoreAuthentication extends IAuthentication {

	public SecurityContext buildContext(String sid, String origin, String domainUid, String userUid) throws ServerFault;

	default SecurityContext buildContext(String sid, String domainUid, String userUid) throws ServerFault {
		return buildContext(sid, "unknown-origin", domainUid, userUid);
	}

	default SecurityContext buildContext(String domainUid, String userUid) throws ServerFault {
		return buildContext(null, "unknown-origin", domainUid, userUid);
	}

	/**
	 * Delete all stored tokens
	 */
	public void resetTokens();

}
