/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
 *
 * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License)
 * or the CeCILL as published by CeCILL.info (version 2 of the License).
 *
 * There are special exceptions to the terms and conditions of the
 * licenses as they are applied to this program. See LICENSE.txt in
 * the directory of this program distribution.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.backend.mail.replica.service.tests;

import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;

public class HttpDbReplicatedMailboxesServiceTests
		extends AbstractReplicatedMailboxesServiceTests<IDbReplicatedMailboxes> {

	protected IDbReplicatedMailboxes getService(SecurityContext ctx) {
		Sessions.get().put("test-sid", ctx);
		return ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", "test-sid")
				.instance(IDbReplicatedMailboxes.class, partition, mboxDescriptor.fullName());
	}

}
